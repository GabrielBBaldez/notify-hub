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

<p align="center">
  <a href="https://gabrielbbaldez.github.io/notify-hub-site/"><strong>Website</strong></a> &nbsp;&middot;&nbsp;
  <a href="https://gabrielbbaldez.github.io/notify-hub-site/docs.html"><strong>Documentation</strong></a> &nbsp;&middot;&nbsp;
  <a href="https://gabrielbbaldez.github.io/notify-hub-site/docs.html#getting-started"><strong>Getting Started</strong></a>
</p>

---

Stop writing different code for each notification channel. NotifyHub gives you a single fluent API to send notifications via **Email, SMS, WhatsApp, Slack, Telegram, Discord, Microsoft Teams, Firebase Push, Webhooks, WebSocket, Google Chat, Twitter/X, LinkedIn, Notion, Twitch, YouTube, Instagram** — or any custom channel you create.

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

| | Problem | Without NotifyHub | With NotifyHub |
|:-:|---------|------------------|----------------|
| <img src="https://cdn.simpleicons.org/gmail" width="16"> | Email | JavaMail config, MIME types, Session... | `.via(EMAIL)` |
| <img src="https://cdn.simpleicons.org/twilio" width="16"> | SMS | Twilio SDK, different API entirely | `.via(SMS)` |
| <img src="https://cdn.simpleicons.org/whatsapp" width="16"> | WhatsApp | Another Twilio setup, prefix logic | `.via(WHATSAPP)` |
| <img src="https://cdn.simpleicons.org/slack" width="16"> | Slack | Webhook HTTP, JSON payload | `.via(SLACK)` |
| <img src="https://cdn.simpleicons.org/telegram" width="16"> | Telegram | Bot API, HTTP client setup | `.via(TELEGRAM)` |
| <img src="https://cdn.simpleicons.org/discord" width="16"> | Discord | Webhook HTTP, JSON payload | `.via(DISCORD)` |
| <img src="https://cdn.simpleicons.org/microsoftteams" width="16"> | Teams | Incoming Webhook, MessageCard JSON | `.via(TEAMS)` |
| <img src="https://cdn.simpleicons.org/firebase" width="16"> | Push | Firebase Admin SDK, credentials... | `.via(PUSH)` |
| <img src="https://cdn.simpleicons.org/webhook" width="16"> | Webhook | Custom HTTP, payload template | `.via(Channel.custom("pagerduty"))` |
| <img src="https://cdn.simpleicons.org/socketdotio" width="16"> | WebSocket | Java WebSocket API, reconnect logic | `.via(WEBSOCKET)` |
| <img src="https://cdn.simpleicons.org/googlechat" width="16"> | Google Chat | Webhook HTTP, JSON payload | `.via(GOOGLE_CHAT)` |
| <img src="https://cdn.simpleicons.org/x/white" width="16"> | Twitter/X | OAuth 1.0a, API v2 setup | `.via(TWITTER)` |
| <img src="https://cdn.simpleicons.org/linkedin" width="16"> | LinkedIn | OAuth 2.0, REST API setup | `.via(LINKEDIN)` |
| <img src="https://cdn.simpleicons.org/notion/white" width="16"> | Notion | Integration Token, API setup | `.via(NOTION)` |
| <img src="https://cdn.simpleicons.org/twitch" width="16"> | Twitch | OAuth 2.0, Twitch API setup | `.via(TWITCH)` |
| <img src="https://cdn.simpleicons.org/youtube" width="16"> | YouTube | YouTube Data API v3 setup | `.via(YOUTUBE)` |
| <img src="https://cdn.simpleicons.org/instagram" width="16"> | Instagram | Meta Graph API setup | `.via(INSTAGRAM)` |
| | Multiple channels | Completely different code for each | Same fluent API |
| Fallback | Manual try/catch chain | `.fallback(SMS)` |
| Retry | Implement yourself | Built-in exponential backoff |
| Async | Thread pools, CompletableFuture | `.sendAsync()` |
| Scheduling | ScheduledExecutor, timer logic | `.schedule(Duration.ofMinutes(30))` |
| Templates | Each channel has its own engine | One template, all channels |
| i18n | Manual locale resolution | `.locale(Locale.PT_BR)` |
| Rate limiting | Token bucket from scratch | Config-driven per-channel |
| Tracking | Build your own delivery log | Built-in receipts + JPA |
| Dead letters | Lost in the void | Auto-captured in DLQ |
| Deduplication | Track sent messages yourself | Built-in content hash / explicit key |
| Template versions | Manage files manually | `.templateVersion("v2")` + A/B test |
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
  - [Message Deduplication](#message-deduplication)
  - [Template Versioning](#template-versioning)
  - [Custom Channels](#custom-channels)
  - [Event Listeners + Spring Events](#event-listeners--spring-events)
  - [Named Recipients](#named-recipients)
  - [Message Queue (RabbitMQ / Kafka)](#message-queue-rabbitmq--kafka)
- [Supported Channels](#supported-channels)
- [Admin Dashboard](#admin-dashboard)
- [Spring Boot Integration](#spring-boot-integration)
- [Configuration Reference](#configuration-reference)
- [Without Spring Boot](#without-spring-boot)
- [MCP Server (AI Agents)](#mcp-server-ai-agents)
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
    <version>0.9.0</version>
</dependency>
```

> **Need extra channels?** Add optional modules:
> ```xml
> <!-- SMS + WhatsApp (Twilio) -->
> <dependency>
>     <groupId>io.github.gabrielbbaldez</groupId>
>     <artifactId>notify-sms</artifactId>
>     <version>0.9.0</version>
> </dependency>
>
> <!-- Slack / Telegram / Discord / Teams / Firebase Push / Webhook -->
> <dependency>
>     <groupId>io.github.gabrielbbaldez</groupId>
>     <artifactId>notify-slack</artifactId>
>     <version>0.9.0</version>
> </dependency>
>
> <!-- WebSocket / Google Chat -->
> <dependency>
>     <groupId>io.github.gabrielbbaldez</groupId>
>     <artifactId>notify-websocket</artifactId>
>     <version>0.9.0</version>
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
    <version>0.9.0</version>
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

### Message Deduplication

Prevent duplicate notifications automatically with content hashing or explicit keys:

```yaml
notify:
  deduplication:
    enabled: true
    ttl: 24h
    strategy: content-hash  # content-hash | explicit-key | both
```

```java
// Auto-dedup by content hash (same recipient + channel + content = skipped)
notify.to(user).via(EMAIL).content("Order confirmed").send();
notify.to(user).via(EMAIL).content("Order confirmed").send(); // skipped!

// Dedup by explicit key
notify.to(user).via(EMAIL)
    .deduplicationKey("order-" + orderId)
    .template("order-confirmed")
    .send();
```

Strategies:
- **`content-hash`** — SHA-256 hash of recipient + channel + subject + content
- **`explicit-key`** — uses the key provided via `.deduplicationKey("...")`
- **`both`** — uses explicit key if provided, otherwise falls back to content hash

Without Spring Boot:

```java
NotifyHub notify = NotifyHub.builder()
    .deduplicationStore(new InMemoryDeduplicationStore(Duration.ofHours(12)))
    .channel(emailChannel)
    .build();
```

### Template Versioning

Manage multiple versions of templates for A/B testing or gradual rollouts:

```
templates/notify/
├── order-confirmed.html           ← default version
├── order-confirmed@v1.html        ← version v1
├── order-confirmed@v2.html        ← version v2
├── order-confirmed_pt_BR@v2.html  ← v2 with i18n
└── order-confirmed.txt            ← text default
```

```java
// Use a specific version
notify.to(user).via(EMAIL)
    .template("order-confirmed")
    .templateVersion("v2")
    .param("orderId", "123")
    .send();

// No version = default template (backward compatible)
notify.to(user).via(EMAIL)
    .template("order-confirmed")
    .send();

// A/B testing
String version = abTestService.getVariant(user, "email-template");
notify.to(user).via(EMAIL)
    .template("welcome")
    .templateVersion(version)  // "v1" or "v2"
    .send();
```

Resolution order: `{name}@{version}_{locale}.{variant}` → `{name}@{version}.{variant}` → `{name}_{locale}.{variant}` → `{name}.{variant}`

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

### Named Recipients

Send notifications to multiple destinations per channel using named aliases. Instead of one hardcoded webhook URL or chat ID, configure as many as you need:

**Configure in `application.yml`:**

```yaml
notify:
  channels:
    discord:
      webhook-url: ${DISCORD_DEFAULT}      # default destination
      username: NotifyHub
      avatar-url: https://example.com/logo.png
      recipients:
        alerts: https://discord.com/api/webhooks/111/aaa
        devops: https://discord.com/api/webhooks/222/bbb
        general: https://discord.com/api/webhooks/333/ccc

    slack:
      webhook-url: ${SLACK_DEFAULT}
      recipients:
        engineering: https://hooks.slack.com/services/XXX/YYY/ZZZ
        marketing: https://hooks.slack.com/services/AAA/BBB/CCC

    telegram:
      bot-token: ${TELEGRAM_BOT_TOKEN}
      chat-id: ${TELEGRAM_DEFAULT_CHAT}
      recipients:
        alerts: "-1001234567890"
        devops: "-1009876543210"
```

**Use with the Java API:**

```java
// Send to a named alias
notify.to("alerts").via(DISCORD).content("Server is down!").send();
notify.to("engineering").via(SLACK).content("Deploy complete").send();
notify.to("devops").via(TELEGRAM).content("CPU at 95%").send();

// Send to default (no alias)
notify.to("user").via(DISCORD).content("Hello!").send();

// Pass a raw URL directly (no alias needed)
notify.to("https://discord.com/api/webhooks/444/ddd").via(DISCORD).content("Direct!").send();
```

**Use with the MCP Server (AI Agents):**

```
send_discord(recipient="alerts", body="Server is down!")
send_slack(recipient="engineering", body="Deploy complete")
send_telegram(recipient="devops", body="CPU at 95%")
```

**Environment variables for MCP/Docker:**

```bash
# Default webhook
NOTIFY_CHANNELS_DISCORD_WEBHOOK_URL=https://discord.com/api/webhooks/111/aaa

# Named recipients (RECIPIENTS_<NAME>)
NOTIFY_CHANNELS_DISCORD_RECIPIENTS_ALERTS=https://discord.com/api/webhooks/222/bbb
NOTIFY_CHANNELS_DISCORD_RECIPIENTS_DEVOPS=https://discord.com/api/webhooks/333/ccc

# Same pattern for all channels
NOTIFY_CHANNELS_SLACK_RECIPIENTS_ENGINEERING=https://hooks.slack.com/services/XXX
NOTIFY_CHANNELS_TELEGRAM_RECIPIENTS_ALERTS=-1001234567890
NOTIFY_CHANNELS_TEAMS_RECIPIENTS_GENERAL=https://outlook.office.com/webhook/XXX
NOTIFY_CHANNELS_GOOGLE_CHAT_RECIPIENTS_TEAM=https://chat.googleapis.com/v1/spaces/XXX
```

**Resolution order:** alias match in recipients map > raw URL/value passthrough > default from config.

Supported on: **Discord, Slack, Telegram, Teams, Google Chat**.

### Message Queue (RabbitMQ / Kafka)

Decouple notification sending with async message queues. NotifyHub provides two modules:

#### RabbitMQ

```xml
<dependency>
    <groupId>io.github.gabrielbbaldez</groupId>
    <artifactId>notify-queue-rabbitmq</artifactId>
    <version>0.9.0</version>
</dependency>
```

```yaml
spring.rabbitmq.host: localhost
spring.rabbitmq.port: 5672

notify.queue.rabbitmq:
  enabled: true
  queue-name: notifyhub-notifications
  exchange-name: notifyhub-exchange
  routing-key: notification
  consumer:
    enabled: true
    concurrency: 1
    max-concurrency: 5
```

```java
@Autowired RabbitNotificationProducer producer;

// Enqueue for async delivery
producer.enqueue(QueuedNotification.builder()
    .recipient("user@example.com")
    .channelName("email")
    .subject("Welcome!")
    .templateName("welcome")
    .params(Map.of("name", "Gabriel"))
    .build());
// Consumer picks it up and sends via NotifyHub automatically
```

#### Apache Kafka

```xml
<dependency>
    <groupId>io.github.gabrielbbaldez</groupId>
    <artifactId>notify-queue-kafka</artifactId>
    <version>0.9.0</version>
</dependency>
```

```yaml
spring.kafka.bootstrap-servers: localhost:9092

notify.queue.kafka:
  enabled: true
  topic: notifyhub-notifications
  consumer:
    enabled: true
    group-id: notifyhub-group
    concurrency: 1
```

```java
@Autowired KafkaNotificationProducer producer;

// Same API as RabbitMQ — just different transport
producer.enqueue(QueuedNotification.builder()
    .recipient("+5548999999999")
    .channelName("sms")
    .rawContent("Your code is 1234")
    .priority("URGENT")
    .build());
```

Both modules support: templates, priority, deduplication keys, delivery tracking, and phone number routing for SMS/WhatsApp.

---

## Supported Channels

| | Channel | Provider | Module |
|:-:|---------|----------|--------|
| <img src="https://cdn.simpleicons.org/gmail" width="18"> | **Email** | SMTP (Gmail, SES, Outlook, any) | `notify-email` |
| <img src="https://cdn.simpleicons.org/twilio" width="18"> | **SMS** | Twilio | `notify-sms` |
| <img src="https://cdn.simpleicons.org/whatsapp" width="18"> | **WhatsApp** | Twilio | `notify-sms` |
| <img src="https://cdn.simpleicons.org/slack" width="18"> | **Slack** | Incoming Webhooks | `notify-slack` |
| <img src="https://cdn.simpleicons.org/telegram" width="18"> | **Telegram** | Bot API | `notify-telegram` |
| <img src="https://cdn.simpleicons.org/discord" width="18"> | **Discord** | Webhooks | `notify-discord` |
| <img src="https://cdn.simpleicons.org/microsoftteams" width="18"> | **Microsoft Teams** | Incoming Webhooks | `notify-teams` |
| <img src="https://cdn.simpleicons.org/firebase" width="18"> | **Push (FCM)** | Firebase Cloud Messaging | `notify-push-firebase` |
| <img src="https://cdn.simpleicons.org/webhook" width="18"> | **Webhook** | Any HTTP endpoint | `notify-webhook` |
| <img src="https://cdn.simpleicons.org/socketdotio" width="18"> | **WebSocket** | JDK WebSocket (`java.net.http`) | `notify-websocket` |
| <img src="https://cdn.simpleicons.org/googlechat" width="18"> | **Google Chat** | Webhooks | `notify-google-chat` |
| <img src="https://cdn.simpleicons.org/x/white" width="18"> | **Twitter/X** | API v2 (OAuth 1.0a) | `notify-twitter` |
| <img src="https://cdn.simpleicons.org/linkedin" width="18"> | **LinkedIn** | REST API (OAuth 2.0) | `notify-linkedin` |
| <img src="https://cdn.simpleicons.org/notion/white" width="18"> | **Notion** | API (Integration Token) | `notify-notion` |
| <img src="https://cdn.simpleicons.org/twitch" width="18"> | **Twitch** | Helix API (OAuth 2.0 auto-refresh) | `notify-twitch` |
| <img src="https://cdn.simpleicons.org/youtube" width="18"> | **YouTube** | Data API v3 (OAuth auto-refresh) | `notify-youtube` |
| <img src="https://cdn.simpleicons.org/instagram" width="18"> | **Instagram** | Meta Graph API | `notify-instagram` |
| 📧 | **SendGrid** | SendGrid API (delivery tracking) | `notify-sendgrid` |
| <img src="https://cdn.simpleicons.org/tiktok/EE1D52" width="18"> | **TikTok Shop** | TikTok Shop API (HMAC-SHA256) | `notify-tiktok-shop` |
| <img src="https://cdn.simpleicons.org/facebook" width="18"> | **Facebook** | Graph API (Page + Messenger) | `notify-facebook` |
| ➕ | **Custom** | Any — implement one interface | `notify-core` |

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
    <version>0.9.0</version>
</dependency>
```

Access at **`/notify-admin`** to see:

- **Dashboard** — metric cards (sent/failed/pending/DLQ/channels/contacts), recent activity feed, system status grid, registered channels
- **Analytics** — Chart.js charts with send volume, channel distribution, success rate, hourly heatmap
- **Tracking** — delivery receipts with channel filter and status badges
- **Dead Letter Queue** — failed notifications with error details and remove action
- **Channels** — status of each registered channel with health indicators
- **Audit Log** — complete history of all notification events with event type filter
- **Audiences** — manage contacts and audience segments with tag-based filtering
- **Status Webhook** — real-time HTTP callback configuration and delivery history

Dark/light theme toggle included — persists across pages via `localStorage`.

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
    "version": "0.9.0",
    "channels": ["email", "slack", "teams"],
    "tracking.enabled": true,
    "dlq.enabled": true
  }
}
```

### OpenTelemetry Tracing

Auto-configured when Micrometer Observation and an OTel bridge are on the classpath.
Add these dependencies to export distributed traces via OTLP:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

Configure the OTLP endpoint in `application.yml`:

```yaml
management:
  tracing:
    sampling:
      probability: 1.0       # 100% sampling (adjust for production)
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces
```

Creates observations (spans):
- `notifyhub.send` (tags: channel, template, outcome)
- `notifyhub.schedule` (tags: channel, outcome)

Compatible with Jaeger, Zipkin, Grafana Tempo, Datadog, and any OTLP-compatible collector.

### Webhook HMAC Signing

The Status Webhook listener supports HMAC-SHA256 request signing for security. When configured, every webhook POST includes a `X-NotifyHub-Signature` header that your server can use to verify the request came from NotifyHub.

```yaml
notify:
  status-webhook:
    url: https://your-server.com/webhook
    signing-secret: ${WEBHOOK_SECRET}   # any secret string
```

Each request includes the header:
```
X-NotifyHub-Signature: sha256=<hex-encoded HMAC-SHA256 of request body>
```

**Verify in your server:**
```java
Mac mac = Mac.getInstance("HmacSHA256");
mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
String expected = "sha256=" + HexFormat.of().formatHex(mac.doFinal(body.getBytes()));
boolean valid = MessageDigest.isEqual(expected.getBytes(), signature.getBytes());
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
      recipients:                          # named aliases (optional)
        engineering: https://hooks.slack.com/services/XXX
        marketing: https://hooks.slack.com/services/YYY

    telegram:
      bot-token: ${TELEGRAM_BOT_TOKEN}
      chat-id: ${TELEGRAM_CHAT_ID}
      recipients:                          # named aliases (optional)
        alerts: "-1001234567890"
        devops: "-1009876543210"

    discord:
      webhook-url: ${DISCORD_WEBHOOK}
      username: NotifyHub
      avatar-url: https://example.com/logo.png
      recipients:                          # named aliases (optional)
        alerts: https://discord.com/api/webhooks/111/aaa
        devops: https://discord.com/api/webhooks/222/bbb

    teams:
      webhook-url: ${TEAMS_WEBHOOK}
      recipients:                          # named aliases (optional)
        general: https://outlook.office.com/webhook/XXX

    push:
      credentials-path: ${FIREBASE_CREDENTIALS}
      project-id: ${FIREBASE_PROJECT_ID}

    webhooks:
      - name: pagerduty
        url: https://events.pagerduty.com/v2/enqueue
        headers:
          Authorization: "Token ${PAGERDUTY_TOKEN}"
        payload-template: '{"summary":"{{content}}"}'

    websocket:
      uri: wss://echo.example.com/ws
      timeout-ms: 10000
      reconnect-enabled: true
      reconnect-delay-ms: 5000
      max-reconnect-attempts: 3
      headers:
        Authorization: "Bearer ${WS_TOKEN}"
      message-format: '{"type":"notification","content":"{{content}}"}'

    google-chat:
      webhook-url: ${GOOGLE_CHAT_WEBHOOK}
      timeout-ms: 10000
      recipients:                          # named aliases (optional)
        team-a: https://chat.googleapis.com/v1/spaces/XXX/messages?key=YYY
        team-b: https://chat.googleapis.com/v1/spaces/ZZZ/messages?key=WWW

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

  deduplication:
    enabled: true
    ttl: 24h
    strategy: content-hash  # content-hash | explicit-key | both

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
            .recipients(Map.of("engineering", "https://hooks.slack.com/services/AAA/BBB/CCC"))
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
    .channel(new WebSocketChannel(
        WebSocketConfig.builder()
            .uri("wss://echo.example.com/ws")
            .messageFormat("{\"text\":\"{{content}}\"}")
            .build()
    ))
    .channel(new GoogleChatChannel(
        GoogleChatConfig.builder()
            .webhookUrl("https://chat.googleapis.com/v1/spaces/XXX/messages?key=YYY")
            .build()
    ))
    .deduplicationStore(new InMemoryDeduplicationStore(Duration.ofHours(24)))
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

## MCP Server (AI Agents)

NotifyHub includes an **MCP (Model Context Protocol) server** that exposes all notification channels as tools for AI agents like **Claude Desktop**, **Claude Code**, **Cursor**, and any MCP-compatible client.

### How it works

The `notify-mcp` module is a standalone Java application that communicates via STDIO using the JSON-RPC protocol. AI agents discover the available tools and can send notifications through any configured channel.

### Setup

**1. Build the MCP server:**

```bash
mvn clean package -pl notify-mcp -am -DskipTests
```

**2. Configure in Claude Desktop** (`claude_desktop_config.json`):

```json
{
  "mcpServers": {
    "notify-hub": {
      "command": "java",
      "args": ["-jar", "path/to/notify-mcp-0.9.0.jar"],
      "env": {
        "NOTIFY_CHANNELS_EMAIL_HOST": "smtp.gmail.com",
        "NOTIFY_CHANNELS_EMAIL_PORT": "587",
        "NOTIFY_CHANNELS_EMAIL_USERNAME": "you@gmail.com",
        "NOTIFY_CHANNELS_EMAIL_PASSWORD": "app-password",
        "NOTIFY_CHANNELS_SLACK_WEBHOOK_URL": "https://hooks.slack.com/...",
        "NOTIFY_CHANNELS_DISCORD_WEBHOOK_URL": "https://discord.com/api/webhooks/..."
      }
    }
  }
}
```

**Or for Claude Code** (`.mcp.json` in project root):

```json
{
  "mcpServers": {
    "notify-hub": {
      "command": "java",
      "args": ["-jar", "path/to/notify-mcp-0.9.0.jar"],
      "env": {
        "NOTIFY_CHANNELS_DISCORD_WEBHOOK_URL": "https://discord.com/api/webhooks/..."
      }
    }
  }
}
```

### Available MCP Tools

| Tool | Description | Required Params |
|------|-------------|-----------------|
| `send_notification` | Send via any channel (generic) | `channel`, `recipient`, `body` or `template` |
| `send_email` | Send email | `to`, `body` or `template` |
| `send_sms` | Send SMS via Twilio | `phone`, `body` or `template` |
| `send_slack` | Send to Slack channel | `recipient`, `body` or `template` |
| `send_telegram` | Send via Telegram Bot | `recipient`, `body` or `template` |
| `send_discord` | Send to Discord channel | `recipient`, `body` or `template` |
| `send_whatsapp` | Send WhatsApp via Twilio | `phone`, `body` or `template` |
| `send_teams` | Send to Microsoft Teams | `recipient`, `body` or `template` |
| `send_google_chat` | Send to Google Chat | `recipient`, `body` or `template` |
| `send_push` | Send push via Firebase | `push_token`, `body` |
| `send_twitter` | Post a tweet on Twitter/X | `body` or `template` |
| `send_linkedin` | Publish a post on LinkedIn | `body` or `template` |
| `send_notion` | Create a page in Notion | `recipient`, `body` or `template` |
| `send_twitch` | Send Twitch chat message + polls | `recipient`, `body` or `template` |
| `send_youtube` | Send YouTube live chat message | `recipient`, `body` or `template` |
| `send_instagram` | Send Instagram DM or feed post | `recipient`, `body` or `template` |
| `send_multi_channel` | Send to multiple channels | `channels[]`, `recipient`, `body` or `template` |
| `send_batch` | Send to multiple recipients at once | `recipients[]`, `channel`, `body` or `template` |
| `send_to_audience` | Send to a named audience | `audience`, `channel`, `body` or `template` |
| `list_channels` | List configured channels | _(none)_ |
| `list_delivery_receipts` | Query delivery history | _(none)_ |
| `list_dead_letters` | View failed notifications (DLQ) | _(none)_ |
| `create_contact` | Create a contact with tags | `name` |
| `list_contacts` | List contacts (filter by tag) | _(none)_ |
| `create_audience` | Create audience with tag filters | `name`, `tags[]` |
| `list_audiences` | List audiences with contact counts | _(none)_ |
| `get_analytics` | Delivery stats by channel/status | _(none)_ |
| `send_tiktok_shop` | Send TikTok Shop notification | `recipient`, `body` or `template` |
| `send_facebook` | Send Facebook page post or Messenger DM | `recipient`, `body` or `template` |
| `check_email_status` | Check SendGrid email delivery status | `message_id` |
| `schedule_notification` | Schedule a notification for later delivery | `channel`, `recipient`, `body`, `send_at` |
| `list_scheduled_notifications` | List all scheduled notifications | _(none)_ |
| `cancel_scheduled_notification` | Cancel a pending scheduled notification | `notification_id` |

All send tools optionally accept: `subject`, `template`, `params`, `priority`.

### Usage example (from an AI agent)

Once configured, you can simply ask your AI agent:

> "Send a Discord message to #alerts saying the deploy is complete"

The agent will call the `send_discord` tool with the appropriate parameters.

### Docker

Run the MCP server without Java installed — only Docker required:

```bash
# Build
docker build -t notifyhub-mcp .

# Run with Discord
docker run -i --rm \
  -e NOTIFY_CHANNELS_DISCORD_WEBHOOK_URL="https://discord.com/api/webhooks/..." \
  -e NOTIFY_CHANNELS_DISCORD_USERNAME="NotifyHub" \
  gabrielbbal10/notifyhub-mcp

# Run with multiple Discord channels + Email
docker run -i --rm \
  -e NOTIFY_CHANNELS_DISCORD_WEBHOOK_URL="https://discord.com/api/webhooks/..." \
  -e NOTIFY_CHANNELS_DISCORD_USERNAME="NotifyHub" \
  -e NOTIFY_CHANNELS_DISCORD_RECIPIENTS_ALERTS="https://discord.com/api/webhooks/111/aaa" \
  -e NOTIFY_CHANNELS_DISCORD_RECIPIENTS_DEVOPS="https://discord.com/api/webhooks/222/bbb" \
  -e NOTIFY_CHANNELS_EMAIL_HOST="smtp.gmail.com" \
  -e NOTIFY_CHANNELS_EMAIL_PORT="587" \
  -e NOTIFY_CHANNELS_EMAIL_USERNAME="you@gmail.com" \
  -e NOTIFY_CHANNELS_EMAIL_PASSWORD="app-password" \
  -e NOTIFY_CHANNELS_EMAIL_FROM="you@gmail.com" \
  gabrielbbal10/notifyhub-mcp
```

**Configure in Claude Code** (`.mcp.json`):

```json
{
  "mcpServers": {
    "notify-hub": {
      "command": "docker",
      "args": ["run", "-i", "--rm",
        "-e", "NOTIFY_CHANNELS_DISCORD_WEBHOOK_URL=https://discord.com/api/webhooks/...",
        "-e", "NOTIFY_CHANNELS_DISCORD_USERNAME=NotifyHub",
        "-e", "NOTIFY_CHANNELS_DISCORD_RECIPIENTS_ALERTS=https://discord.com/api/webhooks/111/aaa",
        "gabrielbbal10/notifyhub-mcp"
      ]
    }
  }
}
```

### Docker REST API

Run a full REST API with Swagger UI — no Java required, just Docker:

```bash
docker run -d -p 8080:8080 \
  -e NOTIFY_CHANNELS_EMAIL_USERNAME=you@gmail.com \
  -e NOTIFY_CHANNELS_EMAIL_PASSWORD=your-app-password \
  -e NOTIFY_CHANNELS_EMAIL_FROM=you@gmail.com \
  gabrielbbal10/notifyhub-api:latest
```

Open [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) for interactive API docs.

**Environment variables — same for both MCP and API images, add only the channels you need:**

| Channel | Variable | Required | Example |
|---------|----------|----------|---------|
| **Email** | `NOTIFY_CHANNELS_EMAIL_HOST` | No (default: `smtp.gmail.com`) | `smtp.gmail.com` |
| | `NOTIFY_CHANNELS_EMAIL_PORT` | No (default: `587`) | `587` |
| | `NOTIFY_CHANNELS_EMAIL_USERNAME` | Yes (for email) | `you@gmail.com` |
| | `NOTIFY_CHANNELS_EMAIL_PASSWORD` | Yes (for email) | `abcd efgh ijkl mnop` ([App Password](https://myaccount.google.com/apppasswords)) |
| | `NOTIFY_CHANNELS_EMAIL_FROM` | Yes (for email) | `you@gmail.com` |
| **Discord** | `NOTIFY_CHANNELS_DISCORD_WEBHOOK_URL` | Yes (for discord) | `https://discord.com/api/webhooks/...` |
| | `NOTIFY_CHANNELS_DISCORD_USERNAME` | No | `NotifyHub` |
| | `NOTIFY_CHANNELS_DISCORD_AVATAR_URL` | No | `https://example.com/avatar.png` |
| | `NOTIFY_CHANNELS_DISCORD_RECIPIENTS_<NAME>` | No | Named alias webhook URL |
| **Slack** | `NOTIFY_CHANNELS_SLACK_WEBHOOK_URL` | Yes (for slack) | `https://hooks.slack.com/services/...` |
| | `NOTIFY_CHANNELS_SLACK_RECIPIENTS_<NAME>` | No | Named alias webhook URL |
| **Telegram** | `NOTIFY_CHANNELS_TELEGRAM_BOT_TOKEN` | Yes (for telegram) | `123456:ABC-DEF...` |
| | `NOTIFY_CHANNELS_TELEGRAM_CHAT_ID` | Yes (for telegram) | `123456789` |
| | `NOTIFY_CHANNELS_TELEGRAM_RECIPIENTS_<NAME>` | No | Named alias chat ID |
| **Google Chat** | `NOTIFY_CHANNELS_GOOGLE_CHAT_WEBHOOK_URL` | Yes (for gchat) | `https://chat.googleapis.com/v1/spaces/...` |
| | `NOTIFY_CHANNELS_GOOGLE_CHAT_RECIPIENTS_<NAME>` | No | Named alias webhook URL |
| **Teams** | `NOTIFY_CHANNELS_TEAMS_WEBHOOK_URL` | Yes (for teams) | `https://outlook.office.com/webhook/...` |
| | `NOTIFY_CHANNELS_TEAMS_RECIPIENTS_<NAME>` | No | Named alias webhook URL |
| **SMS** | `NOTIFY_CHANNELS_SMS_ACCOUNT_SID` | Yes (for sms) | Twilio Account SID |
| | `NOTIFY_CHANNELS_SMS_AUTH_TOKEN` | Yes (for sms) | Twilio Auth Token |
| | `NOTIFY_CHANNELS_SMS_FROM_NUMBER` | Yes (for sms) | `+12025551234` |
| **WhatsApp** | `NOTIFY_CHANNELS_WHATSAPP_ACCOUNT_SID` | Yes (for whatsapp) | Twilio Account SID |
| | `NOTIFY_CHANNELS_WHATSAPP_AUTH_TOKEN` | Yes (for whatsapp) | Twilio Auth Token |
| | `NOTIFY_CHANNELS_WHATSAPP_FROM_NUMBER` | Yes (for whatsapp) | `+14155238886` |

**Example with multiple channels:**

```bash
docker run -d -p 8080:8080 \
  -e NOTIFY_CHANNELS_EMAIL_USERNAME=you@gmail.com \
  -e NOTIFY_CHANNELS_EMAIL_PASSWORD=your-app-password \
  -e NOTIFY_CHANNELS_EMAIL_FROM=you@gmail.com \
  -e NOTIFY_CHANNELS_DISCORD_WEBHOOK_URL="https://discord.com/api/webhooks/..." \
  -e NOTIFY_CHANNELS_DISCORD_USERNAME="NotifyHub" \
  -e NOTIFY_CHANNELS_TELEGRAM_BOT_TOKEN="123456:ABC-DEF..." \
  -e NOTIFY_CHANNELS_TELEGRAM_CHAT_ID="123456789" \
  -e NOTIFY_CHANNELS_GOOGLE_CHAT_WEBHOOK_URL="https://chat.googleapis.com/v1/spaces/..." \
  gabrielbbal10/notifyhub-api:latest
```

**Usage from any language:**

```bash
# Send email
curl -X POST "http://localhost:8080/send/email?to=user@example.com&subject=Hello&body=Hi!"

# Send Discord
curl -X POST "http://localhost:8080/send/discord?message=Deploy done!"

# Send Telegram
curl -X POST "http://localhost:8080/send/telegram?chatId=123456789&message=Alert!"

# Send Google Chat
curl -X POST "http://localhost:8080/send/google-chat?message=Build passed!"
```

**Docker Hub:** [gabrielbbal10/notifyhub-api](https://hub.docker.com/r/gabrielbbal10/notifyhub-api)

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
| `POST` | `/send/teams` | Send to Microsoft Teams via Webhook |
| `POST` | `/send/google-chat` | Send to Google Chat via Webhook |
| `POST` | `/send/push` | Send push notification via Firebase |
| `POST` | `/send/websocket` | Send message via WebSocket |
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
│   ├── MustacheTemplateEngine            # Template engine (i18n-aware + versioning)
│   ├── VersionedTemplateEngine           # Template versioning interface (A/B testing)
│   ├── DeduplicationStore                # Dedup interface (in-memory impl)
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
│   ├── notify-webhook/                   # Generic webhook (configurable)
│   ├── notify-websocket/                 # WebSocket (JDK java.net.http)
│   ├── notify-google-chat/              # Google Chat webhooks (JDK HttpClient)
│   ├── notify-twitch/                   # Twitch chat + polls via Helix API (JDK HttpClient)
│   ├── notify-youtube/                  # YouTube live chat via Data API v3 (JDK HttpClient)
│   └── notify-instagram/               # Instagram DM + feed via Meta Graph API (JDK HttpClient)
│
├── notify-tracker-jpa/                   # JPA-backed delivery tracker
│
├── notify-spring-boot-starter/           # Auto-config for Spring Boot
│   ├── NotifyAutoConfiguration           # Auto-discovers all channels
│   ├── MicrometerNotificationListener    # Metrics (counters per channel)
│   ├── TracingNotificationListener       # OpenTelemetry tracing (Observation API)
│   ├── NotifyHubHealthIndicator          # Actuator health check
│   ├── NotifyHubInfoContributor          # Actuator info endpoint
│   ├── SpringEventNotificationListener   # Spring ApplicationEvents
│   └── NotifyProperties                  # application.yml binding
│
├── notify-admin/                         # Admin dashboard (Thymeleaf)
│   └── NotifyAdminController             # /notify-admin/*
│
├── notify-mcp/                           # MCP Server for AI agents
│   ├── NotifyMcpServer                   # Spring Boot headless main
│   ├── McpServerRunner                   # STDIO MCP server bootstrap
│   └── tools/                            # 27 MCP tools (send, batch, audiences, DLQ, analytics)
│
└── notify-demo/                          # Demo app (run it!)
```

### Design Principles

- **`notify-core` has zero Spring dependency** — use it in any Java project
- **Channels are pluggable** — implement `NotificationChannel`, register as a Spring bean
- **Slack, Telegram, Discord, Teams, WebSocket, Google Chat use zero external SDKs** — only JDK `java.net.http.HttpClient`
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

**What it does:** Auto-configures NotifyHub inside a Spring Boot application. Automatically discovers channel beans, wires retry policies, tracking, rate limiting, DLQ, Micrometer metrics, OpenTelemetry tracing, Actuator health checks, and Spring events. Includes `notify-core` and `notify-email` transitively.

**When to use:** You're building a Spring Boot app and want automatic setup. This is the **only required dependency** for most projects.

```xml
<dependency>
    <groupId>io.github.gabrielbbaldez</groupId>
    <artifactId>notify-spring-boot-starter</artifactId>
    <version>0.9.0</version>
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
    <version>0.9.0</version>
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
    <version>0.9.0</version>
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
    <version>0.9.0</version>
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
    <version>0.9.0</version>
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
    <version>0.9.0</version>
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
    <version>0.9.0</version>
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
    <version>0.9.0</version>
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
    <version>0.9.0</version>
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
    <version>0.9.0</version>
</dependency>
```

---

#### `notify-websocket` — WebSocket Channel

**What it does:** Sends notifications over WebSocket connections using the JDK `java.net.http.WebSocket` API. Supports configurable message format with `{{recipient}}`, `{{subject}}`, `{{content}}` placeholders, custom headers, auto-reconnect with backoff, and connection timeout. Zero external dependencies.

**When to use:** You want to send real-time notifications over WebSocket to a server (e.g., live dashboards, chat systems, or custom WebSocket consumers).

```xml
<dependency>
    <groupId>io.github.gabrielbbaldez</groupId>
    <artifactId>notify-websocket</artifactId>
    <version>0.9.0</version>
</dependency>
```

---

#### `notify-google-chat` — Google Chat Channel

**What it does:** Sends messages to Google Chat spaces via Incoming Webhooks. Posts JSON payloads using the JDK `HttpClient` — no external SDK needed.

**When to use:** You want to send notifications to a Google Chat space. Requires a Google Chat webhook URL (space Settings > Apps & integrations > Webhooks).

```xml
<dependency>
    <groupId>io.github.gabrielbbaldez</groupId>
    <artifactId>notify-google-chat</artifactId>
    <version>0.9.0</version>
</dependency>
```

---

#### `notify-twitch` — Twitch Channel

**What it does:** Sends chat messages and polls to Twitch channels via the Helix API. Uses the JDK `HttpClient` — no external SDK needed.

**When to use:** You want to send notifications to Twitch chat or create polls. Requires Twitch API OAuth 2.0 credentials (Client ID + OAuth token).

```xml
<dependency>
    <groupId>io.github.gabrielbbaldez</groupId>
    <artifactId>notify-twitch</artifactId>
    <version>0.9.0</version>
</dependency>
```

---

#### `notify-youtube` — YouTube Channel

**What it does:** Sends live chat messages to YouTube live streams via the YouTube Data API v3. Uses the JDK `HttpClient` — no external SDK needed.

**When to use:** You want to send notifications to YouTube live chat. Requires a Google API key or OAuth 2.0 credentials with YouTube Data API v3 enabled.

```xml
<dependency>
    <groupId>io.github.gabrielbbaldez</groupId>
    <artifactId>notify-youtube</artifactId>
    <version>0.9.0</version>
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
    <version>0.9.0</version>
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
    <version>0.9.0</version>
</dependency>
```

---

#### `notify-queue-rabbitmq` — RabbitMQ Message Queue

**What it does:** Adds async notification processing via RabbitMQ. Includes a producer (enqueue notifications), a consumer (reads from queue and sends via NotifyHub), and Spring Boot auto-configuration with exchange/queue/binding setup.

**When to use:** You need to decouple notification sending from your main application flow, or you're in a microservice architecture where one service enqueues and another sends.

```xml
<dependency>
    <groupId>io.github.gabrielbbaldez</groupId>
    <artifactId>notify-queue-rabbitmq</artifactId>
    <version>0.9.0</version>
</dependency>
```

---

#### `notify-queue-kafka` — Apache Kafka Message Queue

**What it does:** Same as RabbitMQ module but uses Apache Kafka as the message broker. Sends notifications to a Kafka topic with channel:recipient as the message key for partition ordering.

**When to use:** You're already using Kafka in your infrastructure, or you need high-throughput notification processing at scale.

```xml
<dependency>
    <groupId>io.github.gabrielbbaldez</groupId>
    <artifactId>notify-queue-kafka</artifactId>
    <version>0.9.0</version>
</dependency>
```

---

#### `notify-mcp` — MCP Server for AI Agents

**What it does:** Exposes all NotifyHub channels as MCP (Model Context Protocol) tools, allowing AI agents (Claude Desktop, Claude Code, Cursor) to send notifications through natural language commands. Runs as a headless Spring Boot app communicating via STDIO JSON-RPC. Provides 27 tools: send via any channel, batch send, audience management, DLQ monitoring, and delivery analytics.

**When to use:** You want AI agents to send notifications on your behalf. Configure the JAR path in your MCP client's config file and the agent will discover all available tools automatically.

```xml
<dependency>
    <groupId>io.github.gabrielbbaldez</groupId>
    <artifactId>notify-mcp</artifactId>
    <version>0.9.0</version>
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
| Send via WebSocket | `notify-spring-boot-starter` + `notify-websocket` |
| Send to Google Chat | `notify-spring-boot-starter` + `notify-google-chat` |
| Send to Twitch chat | `notify-spring-boot-starter` + `notify-twitch` |
| Send to YouTube live chat | `notify-spring-boot-starter` + `notify-youtube` |
| Prevent duplicate sends | `notify-spring-boot-starter` (built-in, config-driven) |
| A/B test templates | `notify-spring-boot-starter` (built-in, use `.templateVersion()`) |
| Persist tracking to database | `notify-spring-boot-starter` + `notify-tracker-jpa` |
| Admin dashboard UI | `notify-spring-boot-starter` + `notify-admin` |
| Use without Spring Boot | `notify-core` + channel modules you need |
| Async processing via RabbitMQ | `notify-spring-boot-starter` + `notify-queue-rabbitmq` |
| Async processing via Kafka | `notify-spring-boot-starter` + `notify-queue-kafka` |
| Let AI agents send notifications | `notify-mcp` (standalone JAR) |
| Everything at once | `notify-spring-boot-starter` + all channel modules above |

---

## Roadmap

- [x] **v0.1.0** — Core API, Email, SMS, WhatsApp, Mustache templates, Spring Boot starter
- [x] **v0.2.0** — Slack, Telegram, Discord, async sending, scheduling, delivery tracking
- [x] **v0.3.0** — Teams, Firebase Push, Webhook, attachments, priority, rate limiting, DLQ, i18n, batch send, JPA tracker, Micrometer metrics, Actuator health, Spring events, conditional routing, admin dashboard (80+ tests)
- [x] **v0.4.0** — WebSocket channel, Google Chat channel, message deduplication, template versioning (105+ tests, 16 modules)
- [x] **v0.5.0** — MCP Server module: 13 AI agent tools for sending notifications via Claude Desktop, Claude Code, Cursor (130+ tests, 17 modules)
- [x] **v0.6.0** — Named recipients, Docker images (MCP + REST API), Swagger UI, RabbitMQ + Kafka message queue modules, GitHub Pages landing page
- [x] **v0.7.0** — Twitter/X, LinkedIn, Notion channels (14 channels, 16 MCP tools, 22 modules)
- [x] **v0.8.0** — Twitch, YouTube channels, admin dashboard redesign (16 channels, 18 MCP tools, 24 modules)
- [x] **v0.9.0** — MCP advanced tools: audiences, contacts, batch send, DLQ, analytics (16 channels, 26 MCP tools)
- [x] **v0.10.0** — Instagram channel: DMs and feed posts via Meta Graph API (17 channels, 27 MCP tools, 25 modules)

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
