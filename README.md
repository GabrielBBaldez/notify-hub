# NotifyHub

**One API. Every channel.** Unified notification library for Java and Spring Boot.

[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue)](https://openjdk.org/)
[![Spring Boot 3.x](https://img.shields.io/badge/Spring%20Boot-3.x-green)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Stop writing different code for each notification channel. NotifyHub gives you a single fluent API to send notifications via Email, SMS, WhatsApp, Slack — or any custom channel you create.

```java
notify.to(user)
    .via(EMAIL)
    .fallback(SMS)
    .subject("Order confirmed")
    .template("order-confirmed")
    .param("orderId", order.getId())
    .send();
```

---

## Why NotifyHub?

| Problem | Without NotifyHub | With NotifyHub |
|---------|------------------|----------------|
| Email | JavaMail config, MIME types, Session... | `.via(EMAIL)` |
| SMS | Twilio SDK, different API entirely | `.via(SMS)` |
| WhatsApp | Another Twilio setup, prefix logic | `.via(WHATSAPP)` |
| Multiple channels | Completely different code for each | Same fluent API |
| Fallback | Manual try/catch chain | `.fallback(SMS)` |
| Retry | Implement yourself | Built-in exponential backoff |
| Templates | Each channel has its own engine | One template, all channels |
| New channel | Build from scratch | Implement one interface |

---

## Table of Contents

- [Quick Start](#quick-start)
- [Features](#features)
  - [Fallback Chain](#fallback-chain)
  - [Multi-Channel Send](#multi-channel-send)
  - [Retry with Backoff](#retry-with-backoff)
  - [Templates (Mustache)](#templates-mustache)
  - [Notifiable Interface](#notifiable-interface)
  - [Custom Channels](#custom-channels)
  - [Event Listeners](#event-listeners)
- [Supported Channels](#supported-channels)
- [Configuration Reference](#configuration-reference)
- [Without Spring Boot](#without-spring-boot)
- [Running the Demo](#running-the-demo)
- [Testing with Real Services](#testing-with-real-services)
- [Architecture](#architecture)
- [Publishing to Maven Central](#publishing-to-maven-central)
- [Roadmap](#roadmap)
- [License](#license)

---

## Quick Start

### 1. Add the dependency

```xml
<dependency>
    <groupId>io.notifyhub</groupId>
    <artifactId>notify-spring-boot-starter</artifactId>
    <version>0.1.0</version>
</dependency>
```

> **SMS/WhatsApp?** Add the Twilio channel too:
> ```xml
> <dependency>
>     <groupId>io.notifyhub</groupId>
>     <artifactId>notify-sms</artifactId>
>     <version>0.1.0</version>
> </dependency>
> ```

### 2. Configure in `application.yml`

```yaml
notify:
  channels:
    email:
      host: smtp.gmail.com
      port: 587
      username: ${GMAIL_USER}
      password: ${GMAIL_PASS}
      from: noreply@myapp.com
      from-name: MyApp
      tls: true
  retry:
    max-attempts: 3
    strategy: exponential  # none, fixed, exponential
```

### 3. Inject and use

```java
@Service
public class OrderService {

    private final NotifyHub notify;

    public OrderService(NotifyHub notify) {
        this.notify = notify;
    }

    public void confirmOrder(Order order) {
        notify.to(order.getCustomer())
            .via(Channel.EMAIL)
            .subject("Order confirmed!")
            .template("order-confirmed")
            .param("customerName", order.getCustomer().getName())
            .param("orderId", order.getId())
            .param("total", order.getTotal())
            .send();
    }
}
```

That's it. Three steps.

---

## Features

### Fallback Chain

If the primary channel fails, automatically try the next one:

```java
notify.to(user)
    .via(Channel.WHATSAPP)
    .fallback(Channel.SMS)
    .fallback(Channel.EMAIL)
    .template("payment-reminder")
    .param("amount", "R$ 150,00")
    .send();
// Tries WhatsApp -> SMS -> Email
```

### Multi-Channel Send

Send through ALL channels simultaneously:

```java
notify.to(user)
    .via(Channel.EMAIL)
    .via(Channel.custom("slack"))
    .subject("Security Alert")
    .content("Login from a new device detected")
    .sendAll();
```

### Retry with Backoff

Automatic retry with exponential or fixed backoff:

```yaml
# application.yml (global)
notify:
  retry:
    max-attempts: 3
    strategy: exponential  # waits 1s, 2s, 4s...
```

```java
// Or per-notification
notify.to(user)
    .via(Channel.EMAIL)
    .retry(3)
    .template("invoice")
    .send();
```

### Templates (Mustache)

Create templates in `src/main/resources/templates/notify/`:

**order-confirmed.html** (auto-used for email):
```html
<h1>Hello, {{customerName}}!</h1>
<p>Your order <strong>#{{orderId}}</strong> has been confirmed.</p>
<p>Total: <strong>{{total}}</strong></p>
```

**order-confirmed.txt** (auto-used for SMS/WhatsApp):
```
Hello {{customerName}}, your order #{{orderId}} is confirmed. Total: {{total}}
```

The library picks `.html` for email and `.txt` for SMS/WhatsApp automatically. If only one format exists, it converts.

### Notifiable Interface

Make your User entity a notification recipient:

```java
@Entity
public class User implements Notifiable {

    private String name;
    private String email;
    private String phone;

    @Override
    public String getNotifyEmail() { return email; }

    @Override
    public String getNotifyPhone() { return phone; }

    @Override
    public String getNotifyName() { return name; }
}
```

Then just pass the user object:

```java
notify.to(user)       // resolves email/phone automatically
    .via(Channel.EMAIL)
    .template("welcome")
    .send();
```

Or use raw addresses:

```java
notify.to("user@email.com").via(Channel.EMAIL).content("Hello!").send();
notify.toPhone("+5511999999999").via(Channel.SMS).content("Code: 1234").send();
```

### Custom Channels

Create your own channel by implementing one interface:

```java
@Component
public class SlackChannel implements NotificationChannel {

    @Override
    public String getName() { return "slack"; }

    @Override
    public void send(Notification notification) {
        // POST to Slack webhook, Discord, Telegram, etc.
        webhookClient.post(notification.getRecipient(), notification.getRenderedContent());
    }
}
```

Use it:

```java
notify.to("#team-alerts")
    .via(Channel.custom("slack"))
    .template("deploy-success")
    .param("version", "2.1.0")
    .send();
```

Spring Boot auto-discovers any `NotificationChannel` bean. No extra config needed.

### Event Listeners

Monitor notification outcomes:

```java
@Component
public class NotifyMonitor implements NotificationListener {

    @Override
    public void onSuccess(String channel, String template) {
        metrics.increment("notifications.sent." + channel);
    }

    @Override
    public void onFailure(String channel, String template, Exception error) {
        log.error("Failed on {}: {}", channel, error.getMessage());
        alertService.warn("Channel " + channel + " is failing");
    }
}
```

---

## Supported Channels

| Channel | Provider | Module | Status |
|---------|----------|--------|--------|
| Email | SMTP (any provider) | `notify-email` | ✅ Stable |
| SMS | Twilio | `notify-sms` | ✅ Stable |
| WhatsApp | Twilio | `notify-sms` | ✅ Stable |
| Push | Firebase | planned | Roadmap |
| Custom | Any | `notify-core` | ✅ Stable |

---

## Configuration Reference

Full `application.yml` with all options:

```yaml
notify:
  channels:
    email:
      host: smtp.gmail.com        # SMTP server
      port: 587                    # SMTP port (587 for TLS, 465 for SSL)
      username: ${GMAIL_USER}      # SMTP username
      password: ${GMAIL_PASS}      # SMTP password (use App Passwords for Gmail)
      from: noreply@myapp.com      # Sender address
      from-name: MyApp             # Sender display name
      tls: true                    # Enable STARTTLS (default: true)
      ssl: false                   # Enable SSL (default: false)

    sms:
      account-sid: ${TWILIO_SID}   # Twilio Account SID
      auth-token: ${TWILIO_TOKEN}  # Twilio Auth Token
      from-number: "+1234567890"   # Twilio phone number (E.164 format)

    whatsapp:
      account-sid: ${TWILIO_SID}   # Same Twilio account
      auth-token: ${TWILIO_TOKEN}  # Same Twilio token
      from-number: "+14155238886"  # Twilio WhatsApp sandbox number

  retry:
    max-attempts: 3                # Max retry attempts (default: 1 = no retry)
    strategy: exponential          # none, fixed, exponential (default: none)
```

> **Gmail users:** You need a [Google App Password](https://myaccount.google.com/apppasswords), not your real password. Enable 2FA first.

> **Twilio users:** Get your credentials at [twilio.com/console](https://www.twilio.com/console). For WhatsApp, join the [Twilio Sandbox](https://www.twilio.com/console/sms/whatsapp/sandbox) first.

---

## Without Spring Boot

NotifyHub works without Spring — use the builder directly:

```java
NotifyHub notify = NotifyHub.builder()
    .templateEngine(new MustacheTemplateEngine())
    .channel(new SmtpEmailChannel(
        SmtpConfig.builder()
            .host("smtp.gmail.com")
            .port(587)
            .username("user@gmail.com")
            .password("app-password")
            .from("noreply@myapp.com")
            .tls(true)
            .build()
    ))
    .channel(new TwilioSmsChannel(
        TwilioConfig.builder()
            .accountSid("ACXXXXXXXXXX")
            .authToken("your-token")
            .fromNumber("+1234567890")
            .build()
    ))
    .defaultRetryPolicy(RetryPolicy.exponential(3))
    .build();

notify.to("user@email.com")
    .via(Channel.EMAIL)
    .subject("Hello!")
    .content("Welcome to the app!")
    .send();
```

Only `notify-core` + channel modules needed. No Spring dependency.

---

## Running the Demo

The demo app showcases every feature with a built-in SMTP server — **zero external config needed**.

```bash
# Clone and build
git clone https://github.com/GabrielBBaldez/notify-hub.git
cd notify-hub
mvn clean install -DskipTests

# Run the demo
mvn -pl notify-demo spring-boot:run
```

Then open [http://localhost:8080](http://localhost:8080) to see all available endpoints.

### Demo Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/` | Home — lists all endpoints |
| `POST` | `/send/email?to=x&subject=x&body=x` | Send a simple email |
| `POST` | `/send/template?to=x&customerName=x&orderId=x&total=x` | Send templated email |
| `POST` | `/send/notifiable?name=x&email=x&plan=x` | Send to a Notifiable entity |
| `POST` | `/send/sms?to=+5511...&message=x` | Send SMS (requires Twilio) |
| `POST` | `/send/whatsapp?to=+5511...&message=x` | Send WhatsApp (requires Twilio) |
| `POST` | `/send/slack?channel=%23general&message=x` | Send to Slack channel |
| `POST` | `/send/multi?email=x&slackChannel=x` | Send to email + Slack simultaneously |
| `POST` | `/send/fallback` | Test fallback (email fails -> Slack) |
| `GET` | `/inbox` | View captured emails |
| `GET` | `/inbox/slack` | View Slack messages |
| `DELETE` | `/inbox` | Clear all inboxes |

### Quick Test (default profile)

```bash
# Send an email (captured by embedded SMTP)
curl -X POST "http://localhost:8080/send/email?to=test@test.com&subject=Hello&body=It+works!"

# Check the inbox
curl http://localhost:8080/inbox
```

---

## Testing with Real Services

Want to test with **real Gmail, Twilio SMS, WhatsApp, and Slack**? Use the `real` profile.

### Prerequisites

1. **Gmail:** Enable [2FA](https://myaccount.google.com/security) and create an [App Password](https://myaccount.google.com/apppasswords)
2. **Twilio:** Create a free account at [twilio.com](https://www.twilio.com/try-twilio) (SMS + WhatsApp)
3. **Slack (optional):** Create an [Incoming Webhook](https://api.slack.com/messaging/webhooks)

### Set Environment Variables

```bash
# Gmail
set GMAIL_USER=you@gmail.com
set GMAIL_PASS=xxxx xxxx xxxx xxxx

# Twilio (from twilio.com/console)
set TWILIO_SID=ACXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
set TWILIO_TOKEN=your_auth_token_here
set TWILIO_FROM_NUMBER=+12025551234

# Slack (optional)
set SLACK_WEBHOOK=https://hooks.slack.com/services/XXX/YYY/ZZZ
```

> On Linux/Mac use `export` instead of `set`.

### Run with Real Profile

```bash
mvn -pl notify-demo spring-boot:run -Dspring-boot.run.profiles=real
```

### Test Each Channel

```bash
# Email (arrives in your real inbox)
curl -X POST "http://localhost:8080/send/email?to=you@gmail.com&subject=Real+Test&body=NotifyHub+works!"

# SMS (arrives on your phone)
curl -X POST "http://localhost:8080/send/sms?to=+5511999999999&message=Hello+from+NotifyHub!"

# WhatsApp (join Twilio Sandbox first: twilio.com/console/sms/whatsapp/sandbox)
curl -X POST "http://localhost:8080/send/whatsapp?to=+5511999999999&message=Hello+via+WhatsApp!"

# Slack
curl -X POST "http://localhost:8080/send/slack?channel=%23general&message=Deploy+complete!"
```

> **Note:** Twilio trial accounts only send SMS to [verified numbers](https://www.twilio.com/console/phone-numbers/verified). For WhatsApp, you need to join the [Twilio Sandbox](https://www.twilio.com/console/sms/whatsapp/sandbox) by sending a message to their number first.

---

## Architecture

```
notify-hub/
├── notify-core/                          # Zero Spring dependency
│   ├── NotifyHub                         # Entry point + fluent API
│   ├── NotificationBuilder               # Fluent builder for notifications
│   ├── Notification                      # Immutable notification object
│   ├── Channel / ChannelRef              # Built-in + custom channel refs
│   ├── Notifiable                        # Recipient interface
│   ├── NotificationChannel               # Channel SPI (implement this!)
│   ├── NotificationListener              # Event listener interface
│   ├── MustacheTemplateEngine            # Default template engine
│   └── RetryPolicy                       # Retry + backoff strategies
│
├── notify-channels/
│   ├── notify-email/                     # SMTP email (Jakarta Mail)
│   │   ├── SmtpEmailChannel              # Email channel implementation
│   │   └── SmtpConfig                    # SMTP configuration builder
│   └── notify-sms/                       # Twilio SMS + WhatsApp
│       ├── TwilioSmsChannel              # SMS channel implementation
│       ├── TwilioWhatsAppChannel         # WhatsApp channel implementation
│       └── TwilioConfig                  # Twilio configuration builder
│
├── notify-spring-boot-starter/           # Auto-config for Spring Boot
│   ├── NotifyAutoConfiguration           # Auto-discovers channels + templates
│   ├── NotifySmsAutoConfiguration        # Conditional Twilio auto-config
│   └── NotifyProperties                  # application.yml binding
│
└── notify-demo/                          # Demo app (run it!)
    ├── DemoController                    # REST endpoints for all features
    ├── SlackChannel                      # Custom channel example (webhook)
    ├── EmbeddedSmtpConfig                # GreenMail embedded SMTP
    └── application-real.yml              # Profile for real service testing
```

### Design Principles

- **`notify-core` has zero Spring dependency** — use it in any Java project (Quarkus, Micronaut, plain Java)
- **Channels are pluggable** — implement `NotificationChannel`, register as a Spring bean
- **Template engine is replaceable** — implement `TemplateEngine` interface (default: Mustache)
- **Spring Boot starter auto-configures everything** — just add the dependency
- **Immutable `Notification` objects** — thread-safe by design
- **Conditional auto-config** — SMS/WhatsApp beans only load when Twilio SDK is on classpath

---

## Publishing to Maven Central

> Currently, NotifyHub is available via source/local install. To use it in other projects:

### Option 1: Local Install (development)

```bash
git clone https://github.com/GabrielBBaldez/notify-hub.git
cd notify-hub
mvn clean install
```

Then add the dependency in your project's `pom.xml`:

```xml
<dependency>
    <groupId>io.notifyhub</groupId>
    <artifactId>notify-spring-boot-starter</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Option 2: JitPack (easiest for public GitHub repos)

Add the JitPack repository:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.GabrielBBaldez</groupId>
    <artifactId>notify-hub</artifactId>
    <version>master-SNAPSHOT</version>
</dependency>
```

### Option 3: Maven Central (for production)

See the [Roadmap](#roadmap) — Maven Central publishing is planned for a future release.

---

## Roadmap

- [x] **v0.1.0** — Core API, Email (SMTP), Mustache templates, Spring Boot starter, 30 tests
- [x] **v0.1.1** — SMS (Twilio), WhatsApp (Twilio), Slack (custom channel example), Demo with real service testing
- [ ] **v0.2.0** — Maven Central publishing, Push notifications (Firebase)
- [ ] **v0.3.0** — Async sending, scheduled notifications
- [ ] **v0.4.0** — Broadcast (send to multiple recipients), delivery tracking

---

## Requirements

- **Java 17+**
- **Spring Boot 3.x** (for starter — optional, core works standalone)
- **Maven 3.8+** (for building)

---

## License

MIT License — see [LICENSE](LICENSE) for details.

---

Built by [Gabriel Baldez](https://github.com/GabrielBBaldez)
