<p align="center">
  <img src="docs/logo.png" alt="NotifyHub Mascot" width="180"/>
</p>

<h1 align="center">NotifyHub</h1>

<p align="center">
  <strong>One API. Every channel.</strong><br/>
  Unified notification library for Java and Spring Boot.
</p>

<p align="center">
  <a href="https://openjdk.org/"><img src="https://img.shields.io/badge/Java-17%2B-blue" alt="Java 17+"/></a>
  <a href="https://spring.io/projects/spring-boot"><img src="https://img.shields.io/badge/Spring%20Boot-3.x-green" alt="Spring Boot 3.x"/></a>
  <a href="https://central.sonatype.com/namespace/io.github.gabrielbbaldez"><img src="https://img.shields.io/maven-central/v/io.github.gabrielbbaldez/notify-spring-boot-starter" alt="Maven Central"/></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License: MIT"/></a>
  <a href="https://github.com/GabrielBBaldez/notify-hub/actions/workflows/ci.yml"><img src="https://github.com/GabrielBBaldez/notify-hub/actions/workflows/ci.yml/badge.svg" alt="CI"/></a>
</p>

---

Stop writing different code for each notification channel. NotifyHub gives you a single fluent API to send notifications via **Email, SMS, WhatsApp, Slack, Telegram, Discord, Microsoft Teams, Firebase Push, Webhooks** — or any custom channel you create.

```java
notify.to(user)
    .via(EMAIL)
    .fallback(SMS)
    .priority(Priority.HIGH)
    .subject("Order confirmed")
    .template("order-confirmed")
    .param("orderId", order.getId())
    .attach(invoicePdf)
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
| Teams | Incoming Webhook, MessageCard JSON | `.via(TEAMS)` |
| Push | Firebase Admin SDK, credentials... | `.via(PUSH)` |
| Webhook | Custom HTTP, payload template | `.via(Channel.custom("pagerduty"))` |
| Multiple channels | Completely different code for each | Same fluent API |
| Fallback | Manual try/catch chain | `.fallback(SMS)` |
| Retry | Implement yourself | Built-in exponential backoff |
| Async | Thread pools, CompletableFuture | `.sendAsync()` |
| Scheduling | ScheduledExecutor, timer logic | `.schedule(Duration.ofMinutes(30))` |
| Templates | Each channel has its own engine | One template, all channels |
| i18n | Manual locale resolution | `.locale(Locale.PT_BR)` |
| Rate limiting | Token bucket from scratch | Config-driven per-channel |
| Tracking | Build your own delivery log | Built-in receipts + JPA |
| Dead letters | Lost in the void | Auto-captured in DLQ |
| Batch | Loop and pray | `.toAll(users).send()` |
| Monitoring | Wire Micrometer yourself | Auto-configured counters |
| Health checks | Write an Actuator indicator | Auto-configured |
| Admin UI | Build your own dashboard | Built-in `/notify-admin` |
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
  - [i18n (Internationalization)](#i18n-internationalization)
  - [Attachments](#attachments)
  - [Priority Levels](#priority-levels)
  - [Rate Limiting](#rate-limiting)
  - [Dead Letter Queue (DLQ)](#dead-letter-queue-dlq)
  - [Batch Send](#batch-send)
  - [Delivery Tracking](#delivery-tracking)
  - [Scheduled Notifications](#scheduled-notifications)
  - [Notification Routing](#notification-routing)
  - [Notifiable Interface](#notifiable-interface)
  - [Custom Channels](#custom-channels)
  - [Event Listeners + Spring Events](#event-listeners--spring-events)
- [Supported Channels](#supported-channels)
- [Admin Dashboard](#admin-dashboard)
- [Spring Boot Integration](#spring-boot-integration)
- [Configuration Reference](#configuration-reference)
- [Without Spring Boot](#without-spring-boot)
- [Running the Demo](#running-the-demo)
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
    <version>0.3.0</version>
</dependency>
```

> **Need extra channels?** Add optional modules:
> ```xml
> <!-- SMS + WhatsApp (Twilio) -->
> <dependency>
>     <groupId>io.github.gabrielbbaldez</groupId>
>     <artifactId>notify-sms</artifactId>
>     <version>0.3.0</version>
> </dependency>
>
> <!-- Slack / Telegram / Discord / Teams / Firebase Push / Webhook -->
> <dependency>
>     <groupId>io.github.gabrielbbaldez</groupId>
>     <artifactId>notify-slack</artifactId>
>     <version>0.3.0</version>
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
    strategy: exponential
  tracking:
    enabled: true
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
    .via(Channel.TEAMS)
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

**order-confirmed.txt** (auto-used for SMS/WhatsApp/Slack/Telegram/Discord/Teams):
```
Hello {{customerName}}, your order #{{orderId}} is confirmed. Total: {{total}}
```

The library picks `.html` for email and `.txt` for other channels automatically.

### i18n (Internationalization)

Templates support locale-based resolution with automatic fallback:

```java
// User with locale
notify.to(user)
    .via(Channel.EMAIL)
    .locale(Locale.forLanguageTag("pt-BR"))
    .template("welcome")
    .param("name", user.getName())
    .send();
```

Template resolution order: `welcome_pt_BR.html` -> `welcome_pt.html` -> `welcome.html`

Your `Notifiable` can also return a locale:

```java
public class User implements Notifiable {
    @Override
    public Locale getLocale() {
        return Locale.forLanguageTag("pt-BR");
    }
}
```

### Attachments

Attach files to email notifications:

```java
notify.to(user)
    .via(Channel.EMAIL)
    .subject("Your Invoice")
    .template("invoice")
    .attach("invoice.pdf", pdfBytes, "application/pdf")
    .attach(new File("/reports/monthly.xlsx"))
    .attach(Attachment.fromFile(contractFile))
    .send();
```

### Priority Levels

Set notification priority. **URGENT** notifications bypass rate limiting:

```java
notify.to(user)
    .via(Channel.EMAIL)
    .priority(Priority.URGENT)
    .subject("SERVER DOWN!")
    .content("Production server is unresponsive")
    .send();
```

Available priorities: `URGENT` (bypasses rate limits), `HIGH`, `NORMAL` (default), `LOW`.

### Rate Limiting

Control notification throughput per-channel:

```yaml
notify:
  rate-limit:
    enabled: true
    max-requests: 100
    window: 1m
    channels:
      email:
        max-requests: 50
        window: 1m
      sms:
        max-requests: 10
        window: 1m
```

Rate limiting uses a token bucket algorithm. URGENT priority notifications always bypass rate limits.

### Dead Letter Queue (DLQ)

Failed notifications (after all retries) are automatically captured in the DLQ:

```yaml
notify:
  tracking:
    enabled: true
    dlq-enabled: true
```

View and manage dead letters via the admin dashboard at `/notify-admin/dlq`, or programmatically:

```java
DeadLetterQueue dlq = hub.getDeadLetterQueue();
List<DeadLetter> failed = dlq.findAll();
dlq.remove(deadLetterId); // after manual reprocessing
```

### Batch Send

Send notifications to multiple recipients at once:

```java
// By email addresses
notify.toAll(List.of("user1@test.com", "user2@test.com", "user3@test.com"))
    .via(Channel.EMAIL)
    .subject("System Maintenance")
    .template("maintenance-notice")
    .param("date", "2025-03-01")
    .send();

// By Notifiable entities
notify.toAllNotifiable(users)
    .via(Channel.EMAIL)
    .template("newsletter")
    .send();

// Async batch
notify.toAll(recipients)
    .via(Channel.EMAIL)
    .template("promo")
    .sendAsync();
```

### Delivery Tracking

Track every notification with delivery receipts:

```yaml
notify:
  tracking:
    enabled: true
    type: memory  # or "jpa" for database persistence
```

```java
// Send and get a receipt
DeliveryReceipt receipt = notify.to(user)
    .via(Channel.EMAIL)
    .content("Hello!")
    .sendTracked();

System.out.println(receipt.getStatus());    // SENT
System.out.println(receipt.getId());         // uuid
System.out.println(receipt.getTimestamp());  // 2025-01-15T10:30:00Z
```

For database persistence, add the JPA tracker module:

```xml
<dependency>
    <groupId>io.github.gabrielbbaldez</groupId>
    <artifactId>notify-tracker-jpa</artifactId>
    <version>0.3.0</version>
</dependency>
```

```yaml
notify:
  tracking:
    enabled: true
    type: jpa
```

### Scheduled Notifications

Schedule notifications for future delivery:

```java
ScheduledNotification scheduled = notify.to(user)
    .via(Channel.EMAIL)
    .subject("Reminder")
    .content("Don't forget your appointment tomorrow!")
    .schedule(Duration.ofHours(24));

// Check status
scheduled.getStatus();        // SCHEDULED, SENT, FAILED, CANCELLED
scheduled.getRemainingDelay(); // PT23H59M...

// Cancel if needed
scheduled.cancel();
```

### Notification Routing

Auto-route notifications based on user preferences:

```java
public class User implements Notifiable {
    @Override
    public List<Channel> getPreferredChannels() {
        return List.of(Channel.WHATSAPP, Channel.SMS, Channel.EMAIL);
    }
}

// Auto-routes: WhatsApp (primary) -> SMS (fallback) -> Email (fallback)
notify.notify(user)
    .template("order-update")
    .param("orderId", "12345")
    .send();
```

Conditional routing with rules:

```java
NotificationRouter router = NotificationRouter.builder()
    .rule(RoutingRule.timeBasedRule(
        LocalTime.of(9, 0), LocalTime.of(18, 0),
        Channel.SLACK, Channel.EMAIL))  // Slack during business hours, email after
    .build();
```

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

    @Override
    public Locale getLocale() { return Locale.forLanguageTag("pt-BR"); }

    @Override
    public List<Channel> getPreferredChannels() {
        return List.of(Channel.EMAIL, Channel.SMS);
    }
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

    @Override
    public boolean isAvailable() { return true; }
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

### Event Listeners + Spring Events

Monitor notification outcomes with the listener interface:

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

    @Override
    public void onScheduled(String channel, String recipient, Duration delay) {
        log.info("Scheduled for {} in {}", recipient, delay);
    }
}
```

Or use **Spring Application Events** (auto-configured):

```java
@Component
public class NotificationEventHandler {

    @EventListener
    public void onSent(NotificationSentEvent event) {
        log.info("Sent via {} to {}", event.getChannel(), event.getRecipient());
    }

    @EventListener
    public void onFailed(NotificationFailedEvent event) {
        log.error("Failed: {}", event.getError().getMessage());
    }
}
```

---

## Supported Channels

| Channel | Provider | Module | Status |
|---------|----------|--------|--------|
| Email | SMTP (any provider) | `notify-email` | Stable |
| SMS | Twilio | `notify-sms` | Stable |
| WhatsApp | Twilio | `notify-sms` | Stable |
| Slack | Webhooks | `notify-slack` | Stable |
| Telegram | Bot API | `notify-telegram` | Stable |
| Discord | Webhooks | `notify-discord` | Stable |
| Microsoft Teams | Incoming Webhooks | `notify-teams` | Stable |
| Push (FCM) | Firebase Cloud Messaging | `notify-push-firebase` | Stable |
| Webhook | Any HTTP endpoint | `notify-webhook` | Stable |
| Custom | Any | `notify-core` | Stable |

---

## Admin Dashboard

NotifyHub includes a built-in admin dashboard for monitoring your notification system.

```yaml
notify:
  admin:
    enabled: true
```

```xml
<dependency>
    <groupId>io.github.gabrielbbaldez</groupId>
    <artifactId>notify-admin</artifactId>
    <version>0.3.0</version>
</dependency>
```

Access at **`/notify-admin`** to see:

- **Dashboard** — overview with sent/failed/pending totals, DLQ count, active channels
- **Tracking** — delivery receipts with channel filter
- **Dead Letter Queue** — failed notifications with reprocess action
- **Channels** — status of each registered channel

---

## Spring Boot Integration

### Micrometer Metrics

Auto-configured when Micrometer is on the classpath:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
</dependency>
```

Exposes counters:
- `notifyhub.notifications.sent` (tags: channel, template)
- `notifyhub.notifications.failed` (tags: channel, template)
- `notifyhub.notifications.scheduled` (tags: channel)

### Actuator Health Check

Auto-configured when Spring Boot Actuator is on the classpath:

```
GET /actuator/health/notifyhub
```

```json
{
  "status": "UP",
  "details": {
    "email": "UP",
    "slack": "UP",
    "totalChannels": 2,
    "availableChannels": 2
  }
}
```

Status: **UP** (all channels available), **DEGRADED** (some down), **DOWN** (all down).

### Actuator Info

```
GET /actuator/info
```

```json
{
  "notifyhub": {
    "version": "0.3.0",
    "channels": ["email", "slack", "teams"],
    "tracking.enabled": true,
    "dlq.enabled": true
  }
}
```

---

## Configuration Reference

Full `application.yml` with all options:

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
      ssl: false

    sms:
      account-sid: ${TWILIO_SID}
      auth-token: ${TWILIO_TOKEN}
      from-number: "+1234567890"

    whatsapp:
      account-sid: ${TWILIO_SID}
      auth-token: ${TWILIO_TOKEN}
      from-number: "+14155238886"

    slack:
      webhook-url: ${SLACK_WEBHOOK}

    telegram:
      bot-token: ${TELEGRAM_BOT_TOKEN}
      chat-id: ${TELEGRAM_CHAT_ID}

    discord:
      webhook-url: ${DISCORD_WEBHOOK}
      username: NotifyHub

    teams:
      webhook-url: ${TEAMS_WEBHOOK}

    push:
      credentials-path: ${FIREBASE_CREDENTIALS}
      project-id: ${FIREBASE_PROJECT_ID}

    webhooks:
      - name: pagerduty
        url: https://events.pagerduty.com/v2/enqueue
        headers:
          Authorization: "Token ${PAGERDUTY_TOKEN}"
        payload-template: '{"summary":"{{content}}"}'

  retry:
    max-attempts: 3
    strategy: exponential

  rate-limit:
    enabled: true
    max-requests: 100
    window: 1m
    channels:
      email:
        max-requests: 50
        window: 1m

  tracking:
    enabled: true
    type: memory         # memory | jpa
    dlq-enabled: true

  admin:
    enabled: true
```

---

## Without Spring Boot

NotifyHub works without Spring — use the builder directly:

```java
NotifyHub notify = NotifyHub.builder()
    .templateEngine(new MustacheTemplateEngine())
    .channel(new SmtpEmailChannel(
        SmtpConfig.builder()
            .host("smtp.gmail.com").port(587)
            .username("user@gmail.com").password("app-password")
            .from("noreply@myapp.com").tls(true)
            .build()
    ))
    .channel(new SlackChannel(
        SlackConfig.builder()
            .webhookUrl("https://hooks.slack.com/services/XXX/YYY/ZZZ")
            .build()
    ))
    .channel(new TeamsChannel(
        TeamsConfig.builder()
            .webhookUrl("https://outlook.office.com/webhook/XXX/YYY/ZZZ")
            .build()
    ))
    .channel(new WebhookChannel(
        WebhookConfig.builder()
            .name("pagerduty")
            .url("https://events.pagerduty.com/v2/enqueue")
            .payloadTemplate("{\"summary\":\"{{content}}\"}")
            .build()
    ))
    .defaultRetryPolicy(RetryPolicy.exponential(3))
    .rateLimiter(new TokenBucketRateLimiter(
        RateLimitConfig.perMinute(100)))
    .deadLetterQueue(new InMemoryDeadLetterQueue())
    .tracker(new InMemoryNotificationTracker())
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

// Tracked
DeliveryReceipt receipt = notify.to(user)
    .via(Channel.EMAIL)
    .content("Invoice attached")
    .sendTracked();

// Scheduled
notify.to(user)
    .via(Channel.EMAIL)
    .content("Reminder!")
    .schedule(Duration.ofMinutes(30));

// Batch
notify.toAll(List.of("a@test.com", "b@test.com"))
    .via(Channel.EMAIL)
    .template("announcement")
    .send();
```

Only `notify-core` + channel modules needed. No Spring dependency.

---

## Running the Demo

The demo app showcases every feature with a built-in SMTP server — **zero external config needed**.

```bash
git clone https://github.com/GabrielBBaldez/notify-hub.git
cd notify-hub
mvn clean install -DskipTests

# Run the demo
mvn -pl notify-demo spring-boot:run
```

Then open:
- [http://localhost:8080](http://localhost:8080) — API endpoints
- [http://localhost:8080/notify-admin](http://localhost:8080/notify-admin) — Admin dashboard

### Demo Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/` | Home — lists all endpoints |
| `POST` | `/send/email` | Send a simple email |
| `POST` | `/send/template` | Send email with Mustache template |
| `POST` | `/send/notifiable` | Send to a Notifiable entity |
| `POST` | `/send/sms` | Send SMS (requires Twilio) |
| `POST` | `/send/whatsapp` | Send WhatsApp (requires Twilio) |
| `POST` | `/send/telegram` | Send to Telegram via Bot |
| `POST` | `/send/discord` | Send to Discord via Webhook |
| `POST` | `/send/slack` | Send to Slack channel |
| `POST` | `/send/multi` | Send to email + Slack simultaneously |
| `POST` | `/send/fallback` | Test fallback (email fails -> Slack) |
| `POST` | `/send/tracked` | Send with delivery tracking |
| `POST` | `/send/scheduled` | Schedule notification for future |
| `GET` | `/tracking` | Delivery tracking history |
| `GET` | `/notify-admin` | Admin dashboard |
| `GET` | `/inbox` | View captured emails |
| `DELETE` | `/inbox` | Clear all inboxes |

---

## Architecture

```
notify-hub/
├── notify-core/                          # Zero Spring dependency
│   ├── NotifyHub                         # Entry point + fluent API
│   ├── NotificationBuilder               # Fluent builder (send/async/tracked/scheduled)
│   ├── BatchNotificationBuilder          # Batch send to multiple recipients
│   ├── Notification                      # Immutable notification object
│   ├── Channel / ChannelRef              # Built-in + custom channel refs
│   ├── Priority                          # URGENT, HIGH, NORMAL, LOW
│   ├── Attachment                        # Email file attachments
│   ├── Notifiable                        # Recipient interface (i18n + routing)
│   ├── NotificationChannel               # Channel SPI (implement this!)
│   ├── NotificationListener              # Event listener interface
│   ├── NotificationTracker               # Delivery tracking interface
│   ├── DeadLetterQueue                   # DLQ interface
│   ├── RateLimiter / TokenBucket         # Rate limiting
│   ├── NotificationRouter / RoutingRule  # Conditional routing
│   ├── MustacheTemplateEngine            # Template engine (i18n-aware)
│   └── RetryPolicy                       # Retry + backoff strategies
│
├── notify-channels/
│   ├── notify-email/                     # SMTP email (Jakarta Mail + attachments)
│   ├── notify-sms/                       # Twilio SMS + WhatsApp
│   ├── notify-slack/                     # Slack webhooks (JDK HttpClient)
│   ├── notify-telegram/                  # Telegram Bot API (JDK HttpClient)
│   ├── notify-discord/                   # Discord webhooks (JDK HttpClient)
│   ├── notify-teams/                     # Microsoft Teams webhooks (JDK HttpClient)
│   ├── notify-push-firebase/             # Firebase Cloud Messaging (FCM)
│   └── notify-webhook/                   # Generic webhook (configurable)
│
├── notify-tracker-jpa/                   # JPA-backed delivery tracker
│
├── notify-spring-boot-starter/           # Auto-config for Spring Boot
│   ├── NotifyAutoConfiguration           # Auto-discovers all channels
│   ├── MicrometerNotificationListener    # Metrics (counters per channel)
│   ├── NotifyHubHealthIndicator          # Actuator health check
│   ├── NotifyHubInfoContributor          # Actuator info endpoint
│   ├── SpringEventNotificationListener   # Spring ApplicationEvents
│   └── NotifyProperties                  # application.yml binding
│
├── notify-admin/                         # Admin dashboard (Thymeleaf)
│   └── NotifyAdminController             # /notify-admin/*
│
└── notify-demo/                          # Demo app (run it!)
```

### Design Principles

- **`notify-core` has zero Spring dependency** — use it in any Java project
- **Channels are pluggable** — implement `NotificationChannel`, register as a Spring bean
- **Slack, Telegram, Discord, Teams use zero external SDKs** — only JDK `java.net.http.HttpClient`
- **Template engine is replaceable** — implement `TemplateEngine` interface
- **Spring Boot starter auto-configures everything** — just add the dependency
- **Async support** — `sendAsync()` and `sendAllAsync()` with `CompletableFuture`
- **Conditional auto-config** — channel beans only load when their module is on classpath

---

## Maven Central

NotifyHub is published on **Maven Central**. No extra repositories needed.

Search on Maven Central: [io.github.gabrielbbaldez](https://central.sonatype.com/namespace/io.github.gabrielbbaldez)

### Available Modules

Below is every module, what it does, when you need it, and how to add it.

---

#### `notify-spring-boot-starter` — The Main Dependency

**What it does:** Auto-configures NotifyHub inside a Spring Boot application. Automatically discovers channel beans, wires retry policies, tracking, rate limiting, DLQ, Micrometer metrics, Actuator health checks, and Spring events. Includes `notify-core` and `notify-email` transitively.

**When to use:** You're building a Spring Boot app and want automatic setup. This is the **only required dependency** for most projects.

```xml
<dependency>
    <groupId>io.github.gabrielbbaldez</groupId>
    <artifactId>notify-spring-boot-starter</artifactId>
    <version>0.3.0</version>
</dependency>
```

---

#### `notify-core` — Core API (No Spring)

**What it does:** Contains the entire fluent API (`NotifyHub`, `NotificationBuilder`, `Channel`, `Notification`, `Priority`, `Attachment`, `RetryPolicy`), plus interfaces for channels, templates, tracking, DLQ, rate limiting, and routing. Has **zero Spring dependency** — uses only SLF4J and Mustache.

**When to use:** You want to use NotifyHub in a plain Java project without Spring Boot, or you're building a library/framework on top of it.

```xml
<dependency>
    <groupId>io.github.gabrielbbaldez</groupId>
    <artifactId>notify-core</artifactId>
    <version>0.3.0</version>
</dependency>
```

---

#### `notify-email` — SMTP Email Channel

**What it does:** Sends emails via any SMTP server (Gmail, Outlook, Amazon SES, Mailtrap, etc). Supports HTML and plain text, file attachments, TLS/SSL, and custom sender name. Uses Jakarta Mail internally.

**When to use:** You want to send email notifications. Already included by `notify-spring-boot-starter`.

```xml
<dependency>
    <groupId>io.github.gabrielbbaldez</groupId>
    <artifactId>notify-email</artifactId>
    <version>0.3.0</version>
</dependency>
```

---

#### `notify-sms` — Twilio SMS + WhatsApp Channel

**What it does:** Sends SMS and WhatsApp messages through the Twilio API. Handles phone number formatting (E.164) and the `whatsapp:` prefix automatically.

**When to use:** You need to send SMS or WhatsApp messages. Requires a Twilio account with Account SID, Auth Token, and a phone number.

```xml
<dependency>
    <groupId>io.github.gabrielbbaldez</groupId>
    <artifactId>notify-sms</artifactId>
    <version>0.3.0</version>
</dependency>
```

---

#### `notify-slack` — Slack Webhook Channel

**What it does:** Sends messages to a Slack channel via Incoming Webhooks. Uses the JDK `HttpClient` — no external SDK needed.

**When to use:** You want to post notifications to Slack. Requires a Slack Incoming Webhook URL (created at [api.slack.com/apps](https://api.slack.com/apps)).

```xml
<dependency>
    <groupId>io.github.gabrielbbaldez</groupId>
    <artifactId>notify-slack</artifactId>
    <version>0.3.0</version>
</dependency>
```

---

#### `notify-telegram` — Telegram Bot Channel

**What it does:** Sends messages to Telegram chats/groups/channels via the Bot API. Supports a default chat ID and per-notification targeting. Uses the JDK `HttpClient`.

**When to use:** You want to send Telegram messages. Requires a bot token from [@BotFather](https://t.me/BotFather).

```xml
<dependency>
    <groupId>io.github.gabrielbbaldez</groupId>
    <artifactId>notify-telegram</artifactId>
    <version>0.3.0</version>
</dependency>
```

---

#### `notify-discord` — Discord Webhook Channel

**What it does:** Sends messages to a Discord channel via Webhooks. Supports custom bot username and avatar. Uses the JDK `HttpClient`.

**When to use:** You want to post notifications to Discord. Requires a Discord webhook URL (channel Settings > Integrations > Webhooks).

```xml
<dependency>
    <groupId>io.github.gabrielbbaldez</groupId>
    <artifactId>notify-discord</artifactId>
    <version>0.3.0</version>
</dependency>
```

---

#### `notify-teams` — Microsoft Teams Channel

**What it does:** Sends MessageCard notifications to a Teams channel via Incoming Webhooks. Uses the JDK `HttpClient`.

**When to use:** You want to post notifications to Microsoft Teams. Requires a Teams Incoming Webhook URL (channel > Connectors > Incoming Webhook).

```xml
<dependency>
    <groupId>io.github.gabrielbbaldez</groupId>
    <artifactId>notify-teams</artifactId>
    <version>0.3.0</version>
</dependency>
```

---

#### `notify-push-firebase` — Firebase Cloud Messaging (FCM)

**What it does:** Sends push notifications to mobile devices (Android/iOS) and web apps via Firebase Cloud Messaging. Uses the Firebase Admin SDK with service account credentials.

**When to use:** You want to send push notifications to mobile apps. Requires a Firebase project with a service account JSON credentials file.

```xml
<dependency>
    <groupId>io.github.gabrielbbaldez</groupId>
    <artifactId>notify-push-firebase</artifactId>
    <version>0.3.0</version>
</dependency>
```

---

#### `notify-webhook` — Generic Webhook Channel

**What it does:** Sends notifications to any HTTP endpoint (REST APIs, PagerDuty, Datadog, custom services). Supports configurable payload templates with `{{recipient}}`, `{{subject}}`, `{{content}}` placeholders, custom headers, PUT/POST methods, and timeouts.

**When to use:** You want to integrate with any external service that has an HTTP API, or create custom webhook integrations.

```xml
<dependency>
    <groupId>io.github.gabrielbbaldez</groupId>
    <artifactId>notify-webhook</artifactId>
    <version>0.3.0</version>
</dependency>
```

---

#### `notify-tracker-jpa` — JPA Delivery Tracker

**What it does:** Persists delivery receipts to a relational database (MySQL, PostgreSQL, H2, etc.) using Spring Data JPA. Stores notification ID, channel, recipient, status, timestamp, and error messages. Provides query methods for filtering and counting.

**When to use:** You want delivery tracking data to survive restarts (instead of the default in-memory tracker). Requires Spring Data JPA and a database on the classpath.

```xml
<dependency>
    <groupId>io.github.gabrielbbaldez</groupId>
    <artifactId>notify-tracker-jpa</artifactId>
    <version>0.3.0</version>
</dependency>
```

---

#### `notify-admin` — Admin Dashboard

**What it does:** Provides a built-in web UI at `/notify-admin` with 4 pages: Dashboard (overview metrics), Tracking (delivery receipts), DLQ (failed notifications), and Channels (status). Built with Thymeleaf, dark theme, fully responsive.

**When to use:** You want a visual admin panel to monitor your notification system without building one from scratch. Requires `notify.admin.enabled=true` in your config.

```xml
<dependency>
    <groupId>io.github.gabrielbbaldez</groupId>
    <artifactId>notify-admin</artifactId>
    <version>0.3.0</version>
</dependency>
```

---

### What Do I Need?

| I want to... | Add these dependencies |
|---|---|
| Send emails from Spring Boot | `notify-spring-boot-starter` (already includes email) |
| Send SMS or WhatsApp | `notify-spring-boot-starter` + `notify-sms` |
| Send to Slack | `notify-spring-boot-starter` + `notify-slack` |
| Send to Telegram | `notify-spring-boot-starter` + `notify-telegram` |
| Send to Discord | `notify-spring-boot-starter` + `notify-discord` |
| Send to Microsoft Teams | `notify-spring-boot-starter` + `notify-teams` |
| Send mobile push (FCM) | `notify-spring-boot-starter` + `notify-push-firebase` |
| Send to any HTTP API | `notify-spring-boot-starter` + `notify-webhook` |
| Persist tracking to database | `notify-spring-boot-starter` + `notify-tracker-jpa` |
| Admin dashboard UI | `notify-spring-boot-starter` + `notify-admin` |
| Use without Spring Boot | `notify-core` + channel modules you need |
| Everything at once | `notify-spring-boot-starter` + all channel modules above |

---

## Roadmap

- [x] **v0.1.0** — Core API, Email, SMS, WhatsApp, Mustache templates, Spring Boot starter
- [x] **v0.2.0** — Slack, Telegram, Discord, async sending, scheduling, delivery tracking
- [x] **v0.3.0** — Teams, Firebase Push, Webhook, attachments, priority, rate limiting, DLQ, i18n, batch send, JPA tracker, Micrometer metrics, Actuator health, Spring events, conditional routing, admin dashboard (80+ tests)
- [ ] **v0.4.0** — WebSocket channel, message deduplication, template versioning

---

## Requirements

- **Java 17+**
- **Spring Boot 3.x** (for starter — optional, core works standalone)
- **Maven 3.8+** (for building)

---

## License

MIT License — see [LICENSE](LICENSE) for details.

---

<p align="center">
  Built by <a href="https://github.com/GabrielBBaldez">Gabriel Baldez</a>
</p>
