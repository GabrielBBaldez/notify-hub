# NotifyHub

**One API. Every channel.** Unified notification library for Java and Spring Boot.

Stop writing different code for each notification channel. NotifyHub gives you a single fluent API to send notifications via Email, SMS, WhatsApp, Push, Slack — or any custom channel you create.

```java
notify.to(user)
    .via(EMAIL)
    .fallback(SMS)
    .subject("Order confirmed")
    .template("order-confirmed")
    .param("orderId", order.getId())
    .send();
```

## Why NotifyHub?

| Problem | Without NotifyHub | With NotifyHub |
|---------|------------------|----------------|
| Email | JavaMail config, MIME types, Session... | `.via(EMAIL)` |
| SMS | Twilio SDK, different API entirely | `.via(SMS)` |
| Multiple channels | Completely different code for each | Same fluent API |
| Fallback | Manual try/catch chain | `.fallback(SMS)` |
| Retry | Implement yourself | `.retry(3)` |
| Templates | Each channel has its own engine | One template, all channels |
| New channel | Build from scratch | Implement one interface |

## Quick Start

### 1. Add the dependency

```xml
<dependency>
    <groupId>io.notifyhub</groupId>
    <artifactId>notify-spring-boot-starter</artifactId>
    <version>0.1.0</version>
</dependency>
```

### 2. Configure in application.yml

```yaml
notify:
  channels:
    email:
      host: smtp.gmail.com
      port: 587
      username: ${MAIL_USER}
      password: ${MAIL_PASS}
      from: noreply@myapp.com
      from-name: MyApp
      tls: true
    sms:
      account-sid: ${TWILIO_SID}
      auth-token: ${TWILIO_TOKEN}
      from-number: "+5548999999999"
  retry:
    max-attempts: 3
    strategy: exponential
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
            .via(EMAIL)
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

## Features

### Fallback Chain

If the primary channel fails, automatically try the next one:

```java
notify.to(user)
    .via(WHATSAPP)
    .fallback(SMS)
    .fallback(EMAIL)
    .template("payment-reminder")
    .param("amount", "R$ 150,00")
    .send();
// Tries WhatsApp -> SMS -> Email
```

### Multi-Channel Send

Send through ALL channels simultaneously:

```java
notify.to(user)
    .via(EMAIL, PUSH)
    .template("security-alert")
    .param("action", "Login from new device")
    .sendAll();
```

### Retry with Backoff

Automatic retry with exponential or fixed backoff:

```java
// In application.yml (global)
notify:
  retry:
    max-attempts: 3
    strategy: exponential  # 1s, 2s, 4s

// Or per-notification
notify.to(user)
    .via(EMAIL)
    .template("invoice")
    .retry(3)
    .send();
```

### Templates

Create templates in `resources/templates/notify/`:

**order-confirmed.html** (email):
```html
<h1>Hello, {{customerName}}!</h1>
<p>Your order <strong>#{{orderId}}</strong> has been confirmed.</p>
<p>Total: <strong>{{total}}</strong></p>
```

**order-confirmed.txt** (SMS/WhatsApp):
```
Hello {{customerName}}, your order #{{orderId}} has been confirmed. Total: {{total}}
```

The library automatically picks `.html` for email and `.txt` for SMS/WhatsApp. If only one format exists, it converts automatically.

### Notifiable Interface

Make your User entity a notification recipient:

```java
@Entity
public class User implements Notifiable {

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

Then just pass the user:

```java
notify.to(user)     // resolves email/phone automatically
    .via(EMAIL)
    .template("welcome")
    .send();
```

Or use raw addresses:

```java
notify.to("user@email.com").via(EMAIL).content("Hello!").send();
notify.toPhone("+5548999999999").via(SMS).content("Code: 1234").send();
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
        slackClient.postMessage(
            notification.getRecipient(),
            notification.getRenderedContent()
        );
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

Spring Boot auto-discovers any `NotificationChannel` bean.

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
        log.error("Notification failed on {}: {}", channel, error.getMessage());
        alertService.warn("Channel " + channel + " is failing");
    }
}
```

## Architecture

```
notify-hub/
├── notify-core/                    // Zero Spring dependency
│   ├── NotifyHub                   // Entry point + fluent API
│   ├── Notification                // Immutable notification object
│   ├── NotificationChannel         // Channel interface (SPI)
│   ├── Notifiable                  // Recipient interface
│   ├── TemplateEngine              // Template rendering interface
│   ├── MustacheTemplateEngine      // Default Mustache implementation
│   └── RetryPolicy                 // Retry + backoff strategies
│
├── notify-channels/
│   ├── notify-email/               // SMTP email (Jakarta Mail)
│   └── notify-sms/                 // Twilio SMS
│
└── notify-spring-boot-starter/     // Auto-config for Spring Boot
    ├── NotifyAutoConfiguration     // Auto-discovers channels
    └── NotifyProperties            // application.yml binding
```

### Key Design Decisions

- **notify-core has zero Spring dependency** — use it in any Java project
- **Channels are pluggable** — implement `NotificationChannel` interface
- **Template engine is replaceable** — implement `TemplateEngine` interface
- **Spring Boot starter auto-configures everything** — just add the dependency
- **Immutable Notification objects** — thread-safe by design

## Supported Channels

| Channel | Provider | Module | Status |
|---------|----------|--------|--------|
| Email | SMTP | `notify-email` | v0.1.0 |
| SMS | Twilio | `notify-sms` | v0.2.0 |
| WhatsApp | Twilio / Meta | planned | v0.3.0 |
| Push | Firebase | planned | v0.4.0 |
| Custom | Any | `notify-core` | v0.1.0 |

## Use Cases

**E-commerce** — order confirmation, shipping updates, payment reminders

**Healthcare** — appointment reminders via WhatsApp/SMS with email fallback

**Fintech** — security alerts on all channels simultaneously

**SaaS** — onboarding emails, usage alerts, billing notifications

**Internal tools** — deploy alerts to Slack, incident notifications

## Roadmap

- [x] **v0.1.0** — Core API, Email (SMTP), Mustache templates, Spring Boot starter
- [ ] **v0.2.0** — SMS (Twilio), Fallback chain
- [ ] **v0.3.0** — Retry with backoff, Custom channels SPI
- [ ] **v0.4.0** — WhatsApp (Twilio/Meta), Push (Firebase)
- [ ] **v0.5.0** — Async sending, Scheduling, Broadcast

## Without Spring Boot

NotifyHub works without Spring — just use the builder:

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
    .defaultRetryPolicy(RetryPolicy.exponential(3))
    .build();

notify.to("user@email.com")
    .via(Channel.EMAIL)
    .subject("Hello!")
    .content("Welcome!")
    .send();
```

## Requirements

- Java 17+
- Spring Boot 3.x (for starter, optional)

## License

MIT License - see [LICENSE](LICENSE) for details.

---

Built by [Gabriel Baldez](https://github.com/GabrielBBaldez)
