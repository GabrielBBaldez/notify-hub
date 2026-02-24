# NotifyHub

**One API. Every channel.** Unified notification library for Java and Spring Boot.

[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue)](https://openjdk.org/)
[![Spring Boot 3.x](https://img.shields.io/badge/Spring%20Boot-3.x-green)](https://spring.io/projects/spring-boot)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.gabrielbbaldez/notify-spring-boot-starter)](https://central.sonatype.com/namespace/io.github.gabrielbbaldez)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![CI](https://github.com/GabrielBBaldez/notify-hub/actions/workflows/ci.yml/badge.svg)](https://github.com/GabrielBBaldez/notify-hub/actions/workflows/ci.yml)

Stop writing different code for each notification channel. NotifyHub gives you a single fluent API to send notifications via Email, SMS, WhatsApp, Slack, Telegram, Discord — or any custom channel you create.

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
| Slack | Webhook HTTP, JSON payload | `.via(SLACK)` |
| Telegram | Bot API, HTTP client setup | `.via(TELEGRAM)` |
| Discord | Webhook HTTP, JSON payload | `.via(DISCORD)` |
| Multiple channels | Completely different code for each | Same fluent API |
| Fallback | Manual try/catch chain | `.fallback(SMS)` |
| Retry | Implement yourself | Built-in exponential backoff |
| Async | Thread pools, CompletableFuture | `.sendAsync()` |
| Templates | Each channel has its own engine | One template, all channels |
| New channel | Build from scratch | Implement one interface |

---

## Table of Contents

- [Quick Start](#quick-start)
- [Features](#features)
  - [Fallback Chain](#fallback-chain)
  - [Multi-Channel Send](#multi-channel-send)
  - [Async Sending](#async-sending)
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
- [Maven Central](#maven-central)
- [Roadmap](#roadmap)
- [License](#license)

---

## Quick Start

### 1. Add the dependency

```xml
<dependency>
    <groupId>io.github.gabrielbbaldez</groupId>
    <artifactId>notify-spring-boot-starter</artifactId>
    <version>0.2.0</version>
</dependency>
```

> **SMS/WhatsApp?** Add the Twilio channel too:
> ```xml
> <dependency>
>     <groupId>io.github.gabrielbbaldez</groupId>
>     <artifactId>notify-sms</artifactId>
>     <version>0.2.0</version>
> </dependency>
> ```

> **Slack / Telegram / Discord?** Add the channel you need:
> ```xml
> <dependency>
>     <groupId>io.github.gabrielbbaldez</groupId>
>     <artifactId>notify-slack</artifactId>
>     <version>0.2.0</version>
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
    .via(Channel.SLACK)
    .subject("Security Alert")
    .content("Login from a new device detected")
    .sendAll();
```

### Async Sending

Send notifications without blocking:

```java
// Fire and forget
notify.to(user)
    .via(Channel.EMAIL)
    .template("welcome")
    .sendAsync();

// Or wait for result
CompletableFuture<Void> future = notify.to(user)
    .via(Channel.EMAIL)
    .via(Channel.SLACK)
    .content("Deploy complete!")
    .sendAllAsync();

future.thenRun(() -> log.info("All notifications sent!"));
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

**order-confirmed.txt** (auto-used for SMS/WhatsApp/Slack/Telegram/Discord):
```
Hello {{customerName}}, your order #{{orderId}} is confirmed. Total: {{total}}
```

The library picks `.html` for email and `.txt` for other channels automatically.

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
public class PushChannel implements NotificationChannel {

    @Override
    public String getName() { return "push"; }

    @Override
    public void send(Notification notification) {
        firebaseClient.send(notification.getRecipient(), notification.getRenderedContent());
    }
}
```

Use it:

```java
notify.to(user)
    .via(Channel.custom("push"))
    .template("new-message")
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
| Slack | Webhooks | `notify-slack` | ✅ Stable |
| Telegram | Bot API | `notify-telegram` | ✅ Stable |
| Discord | Webhooks | `notify-discord` | ✅ Stable |
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

    slack:
      webhook-url: ${SLACK_WEBHOOK}  # Slack Incoming Webhook URL

    telegram:
      bot-token: ${TELEGRAM_BOT_TOKEN}  # Telegram Bot token from @BotFather
      chat-id: ${TELEGRAM_CHAT_ID}      # Default chat/group ID (optional)

    discord:
      webhook-url: ${DISCORD_WEBHOOK}   # Discord webhook URL
      username: NotifyHub               # Bot display name (optional)

  retry:
    max-attempts: 3                # Max retry attempts (default: 1 = no retry)
    strategy: exponential          # none, fixed, exponential (default: none)
```

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
    .channel(new SlackChannel(
        SlackConfig.builder()
            .webhookUrl("https://hooks.slack.com/services/XXX/YYY/ZZZ")
            .build()
    ))
    .channel(new TelegramChannel(
        TelegramConfig.builder()
            .botToken("123456:ABC-DEF...")
            .defaultChatId("123456789")
            .build()
    ))
    .defaultRetryPolicy(RetryPolicy.exponential(3))
    .build();

// Sync
notify.to("user@email.com")
    .via(Channel.EMAIL)
    .subject("Hello!")
    .content("Welcome to the app!")
    .send();

// Async
notify.to("#general")
    .via(Channel.SLACK)
    .content("Deploy complete!")
    .sendAsync();
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

---

## Testing with Real Services

Want to test with **real Gmail, Twilio SMS, WhatsApp, and Slack**? Use the `real` profile.

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

---

## Architecture

```
notify-hub/
├── notify-core/                          # Zero Spring dependency
│   ├── NotifyHub                         # Entry point + fluent API
│   ├── NotificationBuilder               # Fluent builder + async (sendAsync)
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
│   ├── notify-sms/                       # Twilio SMS + WhatsApp
│   ├── notify-slack/                     # Slack webhooks (JDK HttpClient)
│   ├── notify-telegram/                  # Telegram Bot API (JDK HttpClient)
│   └── notify-discord/                   # Discord webhooks (JDK HttpClient)
│
├── notify-spring-boot-starter/           # Auto-config for Spring Boot
│   ├── NotifyAutoConfiguration           # Auto-discovers all channels
│   ├── NotifySmsAutoConfiguration        # Conditional Twilio auto-config
│   ├── NotifySlackAutoConfiguration      # Conditional Slack auto-config
│   ├── NotifyTelegramAutoConfiguration   # Conditional Telegram auto-config
│   ├── NotifyDiscordAutoConfiguration    # Conditional Discord auto-config
│   └── NotifyProperties                  # application.yml binding
│
└── notify-demo/                          # Demo app (run it!)
```

### Design Principles

- **`notify-core` has zero Spring dependency** — use it in any Java project
- **Channels are pluggable** — implement `NotificationChannel`, register as a Spring bean
- **Slack, Telegram, Discord use zero external SDKs** — only JDK `java.net.http.HttpClient`
- **Template engine is replaceable** — implement `TemplateEngine` interface
- **Spring Boot starter auto-configures everything** — just add the dependency
- **Async support** — `sendAsync()` and `sendAllAsync()` with `CompletableFuture`
- **Conditional auto-config** — channel beans only load when their module is on classpath

---

## Maven Central

NotifyHub is published on **Maven Central**. No extra repositories needed — just add the dependency:

```xml
<dependency>
    <groupId>io.github.gabrielbbaldez</groupId>
    <artifactId>notify-spring-boot-starter</artifactId>
    <version>0.2.0</version>
</dependency>
```

Available modules:

| Module | Description |
|--------|-------------|
| `notify-spring-boot-starter` | Spring Boot auto-config (includes email) |
| `notify-core` | Core API only (no Spring) |
| `notify-email` | SMTP email channel |
| `notify-sms` | Twilio SMS + WhatsApp |
| `notify-slack` | Slack webhooks |
| `notify-telegram` | Telegram Bot API |
| `notify-discord` | Discord webhooks |

Search on Maven Central: [io.github.gabrielbbaldez](https://central.sonatype.com/namespace/io.github.gabrielbbaldez)

---

## Roadmap

- [x] **v0.1.0** — Core API, Email, SMS, WhatsApp, Mustache templates, Spring Boot starter, published on Maven Central
- [x] **v0.2.0** — Slack, Telegram, Discord channels, async sending (`sendAsync`/`sendAllAsync`), GitHub Actions CI/CD, 43 tests
- [ ] **v0.3.0** — Scheduled notifications, delivery tracking
- [ ] **v0.4.0** — Broadcast (send to multiple recipients), Firebase Push

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
