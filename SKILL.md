---
name: notifyhub
description: |
  Expert skill for NotifyHub — a unified Java 17+ / Spring Boot 3.x notification library
  that sends notifications across 17 channels (Email, SMS, WhatsApp, Slack, Telegram, Discord,
  Teams, Firebase Push, Webhooks, WebSocket, Google Chat, Twitter/X, LinkedIn, Notion, Twitch,
  YouTube, Instagram) through a single fluent API.

  Use this skill whenever the user is working with NotifyHub, notify-hub, or any of its modules
  (notify-core, notify-spring-boot-starter, notify-mcp, notify-channels/*). Also use when the
  user mentions sending notifications in Java/Spring Boot, configuring notification channels,
  fluent notification API, MCP notification tools, or multi-channel messaging. Trigger even if
  the user just says "notifications", "send email/sms/slack/telegram", "notify users", or
  references any NotifyHub class like NotifyHub, NotificationChannel, Notifiable, DeliveryReceipt,
  OAuthTokenManager, RateLimitConfig, etc.
---

# NotifyHub — Unified Notification Library for Java & Spring Boot

You are an expert in **NotifyHub**, a unified notification library for Java 17+ and Spring Boot 3.x that provides a single fluent API to send notifications across 17 channels.

## Project Overview

NotifyHub eliminates the need for different code per notification channel. One fluent API handles Email, SMS, WhatsApp, Slack, Telegram, Discord, Microsoft Teams, Firebase Push, Webhooks, WebSocket, Google Chat, Twitter/X, LinkedIn, Notion, Twitch, YouTube, and Instagram.

- **Group ID**: `io.github.gabrielbbaldez`
- **Version**: `0.9.0`
- **Java**: 17+
- **Spring Boot**: 3.x (optional — works standalone)
- **License**: MIT
- **Template Engine**: Mustache (built-in)
- **Build Tool**: Maven (multi-module)

---

## Project Structure

```
notify-hub/
├── notify-core/                          # Core API, interfaces, fluent builder
│   └── io.notifyhub.core
│       ├── NotifyHub.java                # Main entry point (fluent API)
│       ├── Notification.java             # Immutable notification object
│       ├── Channel.java                  # Channel enum (EMAIL, SMS, SLACK, ...)
│       ├── Priority.java                 # URGENT, HIGH, NORMAL, LOW
│       ├── DeliveryReceipt.java          # Delivery tracking receipt
│       ├── channel/                      # NotificationChannel interface
│       ├── template/                     # TemplateEngine, MustacheTemplateEngine
│       ├── retry/                        # RetryPolicy (none, fixed, exponential)
│       ├── ratelimit/                    # RateLimitConfig, TokenBucketRateLimiter
│       ├── dlq/                          # DeadLetter, DeadLetterQueue
│       ├── oauth/                        # OAuthTokenManager (auto-refresh)
│       ├── audience/                     # Contact, Audience, AudienceManager
│       └── routing/                      # NotificationRouter
├── notify-channels/                      # One module per channel
│   ├── notify-email/                     # SMTP (JavaMail)
│   ├── notify-sms/                       # Twilio SMS
│   ├── notify-slack/                     # Slack webhooks
│   ├── notify-telegram/                  # Telegram Bot API
│   ├── notify-discord/                   # Discord webhooks
│   ├── notify-teams/                     # Microsoft Teams webhooks
│   ├── notify-push-firebase/             # Firebase Cloud Messaging
│   ├── notify-webhook/                   # Generic HTTP webhooks
│   ├── notify-websocket/                 # Java WebSocket
│   ├── notify-google-chat/               # Google Chat webhooks
│   ├── notify-twitter/                   # Twitter/X API v2
│   ├── notify-linkedin/                  # LinkedIn REST API
│   ├── notify-notion/                    # Notion Database API
│   ├── notify-twitch/                    # Twitch Helix API
│   ├── notify-youtube/                   # YouTube Live Chat API
│   └── notify-instagram/                 # Instagram Graph API
├── notify-spring-boot-starter/           # Auto-configuration + properties
│   └── io.notifyhub.spring
│       ├── NotifyAutoConfiguration.java  # Main auto-config
│       ├── NotifyProperties.java         # All Spring Boot properties
│       └── Notify*AutoConfiguration.java # Per-channel auto-configs
├── notify-mcp/                           # MCP Server for AI agents (27 tools)
└── notify-demo/                          # Demo application
```

---

## Core Fluent API

### Entry Points

```java
// Single recipient
notify.to(user)                           // Notifiable object
notify.to("user@example.com")             // Email string
notify.toPhone("+5548999999999")           // Phone number

// Batch recipients
notify.toAll(List.of("a@x.com", "b@x.com"))
notify.toAllNotifiable(List.of(user1, user2))
notify.toAudience("premium-users")

// Auto-route via preferred channels
notify.notify(user)
```

### Fluent Builder Chain

```java
notify.to(user)
    .via(Channel.EMAIL)                   // Primary channel
    .fallback(Channel.SMS)                // Fallback if primary fails
    .fallback(Channel.WHATSAPP)           // Second fallback
    .priority(Priority.HIGH)              // URGENT, HIGH, NORMAL, LOW
    .subject("Order confirmed")           // Subject (email/push/teams)
    .content("Your order #123 is ready")  // Plain text body
    .template("order-confirmed")          // OR use Mustache template
    .templateVersion("v2")                // Template version (A/B testing)
    .param("orderId", order.getId())      // Template parameter
    .params(Map.of("k", "v"))             // Bulk template params
    .locale(Locale.PT_BR)                 // i18n locale
    .attach(file)                         // File attachment (email)
    .retry(3)                             // Override retry count
    .deduplicationKey("order-123")        // Prevent duplicates
    .metadata("traceId", "abc-123")       // Custom metadata
    .send();                              // Execute synchronously
```

### Execution Methods

```java
.send()              // Sync, uses fallback chain
.sendAll()           // Sync, sends to ALL .via() channels
.sendAsync()         // Async (CompletableFuture<Void>)
.sendAllAsync()      // Async to all channels
.sendTracked()       // Returns DeliveryReceipt
.sendAllTracked()    // Returns List<DeliveryReceipt>
.schedule(Duration.ofMinutes(30))   // Schedule for later
.scheduleAt(Instant.parse("..."))   // Schedule at specific time
```

### Channel Enum

```java
Channel.EMAIL, Channel.SMS, Channel.WHATSAPP, Channel.PUSH
Channel.SLACK, Channel.TELEGRAM, Channel.DISCORD, Channel.TEAMS
Channel.WEBSOCKET, Channel.GOOGLE_CHAT
Channel.TWITTER, Channel.LINKEDIN, Channel.NOTION
Channel.TWITCH, Channel.YOUTUBE, Channel.INSTAGRAM
Channel.custom("pagerduty")  // Custom channels
```

### Priority

```java
Priority.URGENT  // Bypasses rate limiting
Priority.HIGH
Priority.NORMAL  // Default
Priority.LOW
```

---

## NotifyHub Builder (Standalone / Advanced)

```java
NotifyHub hub = NotifyHub.builder()
    .channel(emailChannel)
    .channel(slackChannel)
    .channels(List.of(smsChannel, pushChannel))
    .templateEngine(new MustacheTemplateEngine())
    .defaultRetryPolicy(RetryPolicy.exponential(3))
    .listener(myListener)
    .executor(Executors.newFixedThreadPool(4))
    .scheduler(Executors.newScheduledThreadPool(2))
    .tracker(new InMemoryNotificationTracker())
    .rateLimiter(new TokenBucketRateLimiter(config))
    .deadLetterQueue(new InMemoryDeadLetterQueue())
    .router(myRouter)
    .deduplicationStore(new InMemoryDeduplicationStore())
    .auditLog(new InMemoryAuditLog())
    .audienceManager(new AudienceManager(contactRepo))
    .build();
```

---

## Channel Configuration (Java API)

### Email (SMTP)

```java
SmtpConfig.builder()
    .host("smtp.gmail.com").port(587)
    .username("user").password("pass")
    .from("noreply@app.com").fromName("MyApp")
    .tls(true)
    .build();
```

### SMS / WhatsApp (Twilio)

```java
TwilioConfig.builder()
    .accountSid("AC...").authToken("token")
    .fromNumber("+15551234567")
    .build();
```

### Slack

```java
SlackConfig.builder()
    .webhookUrl("https://hooks.slack.com/services/...")
    .recipients(Map.of("engineering", "https://hooks.slack.com/..."))
    .build();
```

### Telegram

```java
TelegramConfig.builder()
    .botToken("123456:ABC-DEF").chatId("987654321")
    .recipients(Map.of("admin", "123456789"))
    .build();
```

### Discord

```java
DiscordConfig.builder()
    .webhookUrl("https://discord.com/api/webhooks/...")
    .username("NotifyBot").avatarUrl("https://...")
    .recipients(Map.of("alerts", "https://discord.com/api/webhooks/..."))
    .build();
```

### Microsoft Teams

```java
TeamsConfig.builder()
    .webhookUrl("https://outlook.office.com/webhook/...")
    .recipients(Map.of("devops", "https://outlook.office.com/webhook/..."))
    .build();
```

### Firebase Push

```java
FirebasePushConfig.builder()
    .serverKey("AAAA...").build();
```

### WebSocket

```java
WebSocketConfig.builder()
    .uri("wss://ws.example.com/notifications")
    .reconnectEnabled(true).reconnectDelayMs(3000).maxReconnectAttempts(10)
    .headers(Map.of("Authorization", "Bearer token"))
    .messageFormat("json")
    .build();
```

### Google Chat

```java
GoogleChatConfig.builder()
    .webhookUrl("https://chat.googleapis.com/v1/spaces/...")
    .recipients(Map.of("team", "https://chat.googleapis.com/..."))
    .build();
```

### Twitter/X

```java
TwitterConfig.builder()
    .apiKey("key").apiSecret("secret")
    .accessToken("token").accessTokenSecret("tokenSecret")
    .build();
```

### LinkedIn

```java
LinkedInConfig.builder()
    .accessToken("token")
    .authorId("urn:li:person:abc123")
    .build();
```

### Notion

```java
NotionConfig.builder()
    .apiKey("secret_...").databaseId("db-id")
    .recipients(Map.of("tasks", "another-db-id"))
    .build();
```

### Twitch (with OAuth auto-refresh)

```java
TwitchConfig.builder()
    .clientId("tyxy0tn8...")
    .accessToken("ck6uie0g...")       // Seed token
    .refreshToken("pmn0lc2x...")      // Optional: enables auto-refresh
    .clientSecret("wku9i8gu...")      // Required with refreshToken
    .broadcasterId("836211004")
    .senderId("836211004")
    .build();
// getAccessToken() auto-renews 5 minutes before expiry
```

### YouTube (with OAuth auto-refresh)

```java
YouTubeConfig.builder()
    .accessToken("ya29.a0...")        // Seed token
    .refreshToken("1//0eXyz...")      // Optional: enables auto-refresh
    .clientId("123.apps.google...")    // Required with refreshToken
    .clientSecret("GOCSPX-...")       // Required with refreshToken
    .channelId("UC...")
    .liveChatId("Cg0KC...")
    .build();
// Token auto-refreshes via Google OAuth2 endpoint
```

### Instagram

```java
InstagramConfig.builder()
    .accessToken("IGQ...")
    .igUserId("17841...")
    .recipients(Map.of("support", "user-id"))
    .build();
```

### Generic Webhook

```java
WebhookConfig.builder()
    .name("pagerduty")
    .url("https://events.pagerduty.com/v2/enqueue")
    .method("POST")
    .headers(Map.of("Authorization", "Token token=..."))
    .payloadTemplate("{\"routing_key\":\"R0...\",\"event_action\":\"trigger\",\"payload\":{\"summary\":\"{{content}}\"}}")
    .build();
```

---

## OAuth Token Refresh

NotifyHub supports automatic OAuth 2.0 token refresh for YouTube and Twitch. The `OAuthTokenManager` is thread-safe using `ReentrantReadWriteLock` and proactively refreshes tokens 5 minutes before expiry.

```java
OAuthTokenManager manager = OAuthTokenManager.builder()
    .tokenEndpoint("https://oauth2.googleapis.com/token")
    .refreshToken("1//0eXyz...")
    .clientId("client-id")
    .clientSecret("client-secret")
    .initialAccessToken("ya29...")  // Optional seed token
    .refreshBufferSeconds(300)      // Default: 300 (5 min before expiry)
    .timeoutSeconds(10)             // HTTP timeout
    .build();

String token = manager.getAccessToken();  // Auto-refresh if needed
manager.forceRefresh();                   // Force refresh (e.g., after 401)
```

Token refresh is enabled automatically when `refreshToken` + `clientSecret` (and `clientId` for YouTube) are provided in the channel config. If only a static `accessToken` is set, behavior is backward-compatible with no refresh.

---

## Templates (Mustache)

Place templates in `src/main/resources/templates/notify/`:

```
templates/notify/
├── order-confirmed/
│   ├── html.mustache          # Email HTML variant
│   └── txt.mustache           # SMS/WhatsApp text variant
├── welcome/
│   ├── html.mustache
│   └── txt.mustache
└── alert/
    └── txt.mustache
```

Usage:

```java
notify.to(user)
    .via(Channel.EMAIL)
    .template("order-confirmed")
    .param("customerName", "Gabriel")
    .param("orderId", "ORD-123")
    .locale(Locale.PT_BR)           // i18n support
    .templateVersion("v2")          // A/B testing
    .send();
```

The `TemplateEngine` interface:

```java
public interface TemplateEngine {
    String render(String templateName, String variant, Map<String, Object> params);
    String render(String templateName, String variant, Map<String, Object> params, Locale locale);
    boolean exists(String templateName, String variant);
}
```

Built-in implementations: `MustacheTemplateEngine`, `VersionedTemplateEngine`.

---

## Retry Policies

```java
RetryPolicy.none()                          // Fail immediately
RetryPolicy.fixed(3, Duration.ofSeconds(2)) // 3 attempts, 2s delay
RetryPolicy.exponential(3)                  // 3 attempts, 1s/2s/4s backoff
RetryPolicy.exponential(5, Duration.ofMillis(500)) // Custom initial delay
```

Methods: `getMaxAttempts()`, `getInitialDelay()`, `getStrategy()` (NONE/FIXED/EXPONENTIAL), `getDelayForAttempt(int)`, `shouldRetry()`.

---

## Rate Limiting

Built-in `TokenBucketRateLimiter` with per-channel API-aware presets:

| Channel      | Limit         | Window    |
|--------------|---------------|-----------|
| YouTube      | 180 requests  | 1 day     |
| Twitch       | 20 requests   | 30 seconds|
| Twitter/X    | 300 requests  | 3 hours   |
| Slack        | 1 request     | 1 second  |
| Telegram     | 30 requests   | 1 second  |
| Discord      | 30 requests   | 1 minute  |
| LinkedIn     | 100 requests  | 1 day     |
| Instagram    | 200 requests  | 1 hour    |
| Google Chat  | 60 requests   | 1 minute  |

```java
RateLimitConfig.youtube()                   // Factory method per channel
RateLimitConfig.forChannel("twitch")        // Lookup by name
RateLimitConfig.allDefaults()               // Map of all presets
RateLimitConfig.perSecond(10)               // Custom
RateLimitConfig.perMinute(60)               // Custom
```

`Priority.URGENT` bypasses rate limiting.

---

## Delivery Tracking

```java
DeliveryReceipt receipt = notify.to(user)
    .via(Channel.EMAIL)
    .template("welcome")
    .sendTracked();

receipt.getId();          // UUID
receipt.getChannelName(); // "email"
receipt.getRecipient();   // "user@example.com"
receipt.getStatus();      // PENDING, SCHEDULED, SENT, FAILED, CANCELLED
receipt.getTimestamp();    // Instant
receipt.getErrorMessage(); // null or error details
```

---

## Dead Letter Queue (DLQ)

Failed notifications (after all retries) are captured in the DLQ:

```java
DeadLetter.builder()
    .channelName("email")
    .recipient("user@example.com")
    .subject("Welcome")
    .content("Hello!")
    .errorMessage("Connection refused")
    .attemptCount(3)
    .build();
```

Access: `hub.getDeadLetterQueue().findAll()`.

---

## Audience & Contact Management

```java
// Create contacts
Contact vip = Contact.builder()
    .name("Gabriel").email("gabriel@example.com").phone("+5548999999999")
    .tag("vip").tag("plan:premium")
    .metadata("region", "south")
    .build();

// Create audience (AND logic on tags)
hub.getAudienceManager().createAudience("premium-users", Set.of("vip", "plan:premium"));

// Send to audience
notify.toAudience("premium-users")
    .via(Channel.EMAIL)
    .template("exclusive-offer")
    .send();
```

---

## Notifiable Interface

Implement on your domain objects:

```java
public class User implements Notifiable {
    public String getNotifyEmail()    { return email; }
    public String getNotifyPhone()    { return phone; }
    public String getNotifyPushToken(){ return pushToken; }
    public String getNotifyName()     { return name; }
    public List<Channel> getPreferredChannels() {
        return List.of(Channel.EMAIL, Channel.PUSH);
    }
}
```

---

## Custom Channels

Implement `NotificationChannel`:

```java
public class PagerDutyChannel implements NotificationChannel {
    public String getName() { return "pagerduty"; }
    public boolean isAvailable() { return apiKey != null; }
    public void send(Notification notification) throws NotificationSendException {
        // Your implementation
    }
}
```

Register: `hub.registerChannel(new PagerDutyChannel(config))` or as a Spring `@Bean`.

---

## Event Listeners

```java
public class MyListener implements NotificationListener {
    public void onSuccess(String channel, String template) { /* ... */ }
    public void onFailure(String channel, String template, Exception e) { /* ... */ }
    public void onScheduled(String channel, String recipient, Duration delay) { /* ... */ }
    public void onCancelled(String channel, String recipient) { /* ... */ }
}
```

---

## Spring Boot Integration

### Dependency

```xml
<dependency>
    <groupId>io.github.gabrielbbaldez</groupId>
    <artifactId>notify-spring-boot-starter</artifactId>
    <version>0.9.0</version>
</dependency>
```

Optional channel modules (add as needed):

```xml
<artifactId>notify-sms</artifactId>           <!-- SMS + WhatsApp -->
<artifactId>notify-slack</artifactId>
<artifactId>notify-telegram</artifactId>
<artifactId>notify-discord</artifactId>
<artifactId>notify-teams</artifactId>
<artifactId>notify-push-firebase</artifactId>
<artifactId>notify-webhook</artifactId>
<artifactId>notify-websocket</artifactId>
<artifactId>notify-google-chat</artifactId>
<artifactId>notify-twitter</artifactId>
<artifactId>notify-linkedin</artifactId>
<artifactId>notify-notion</artifactId>
<artifactId>notify-twitch</artifactId>
<artifactId>notify-youtube</artifactId>
<artifactId>notify-instagram</artifactId>
```

### Full `application.yml` Reference

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
      # ssl: false

    sms:
      account-sid: ${TWILIO_ACCOUNT_SID}
      auth-token: ${TWILIO_AUTH_TOKEN}
      from-number: "+15551234567"

    whatsapp:
      account-sid: ${TWILIO_ACCOUNT_SID}
      auth-token: ${TWILIO_AUTH_TOKEN}
      from-number: "+14155238886"

    slack:
      webhook-url: ${SLACK_WEBHOOK_URL}
      recipients:
        engineering: "https://hooks.slack.com/services/..."
        alerts: "https://hooks.slack.com/services/..."

    telegram:
      bot-token: ${TELEGRAM_BOT_TOKEN}
      chat-id: ${TELEGRAM_CHAT_ID}
      recipients:
        admin: "123456789"

    discord:
      webhook-url: ${DISCORD_WEBHOOK_URL}
      username: NotifyBot
      avatar-url: "https://example.com/avatar.png"
      recipients:
        alerts: "https://discord.com/api/webhooks/..."

    teams:
      webhook-url: ${TEAMS_WEBHOOK_URL}
      recipients:
        devops: "https://outlook.office.com/webhook/..."

    push:
      server-key: ${FIREBASE_SERVER_KEY}

    websocket:
      uri: wss://ws.example.com/notifications
      reconnect-enabled: true
      reconnect-delay-ms: 3000
      max-reconnect-attempts: 10
      headers:
        Authorization: "Bearer ${WS_TOKEN}"

    google-chat:
      webhook-url: ${GOOGLE_CHAT_WEBHOOK_URL}
      recipients:
        team: "https://chat.googleapis.com/..."

    twitter:
      api-key: ${TWITTER_API_KEY}
      api-secret: ${TWITTER_API_SECRET}
      access-token: ${TWITTER_ACCESS_TOKEN}
      access-token-secret: ${TWITTER_ACCESS_TOKEN_SECRET}

    linkedin:
      access-token: ${LINKEDIN_ACCESS_TOKEN}
      author-id: "urn:li:person:abc123"

    notion:
      api-key: ${NOTION_API_KEY}
      database-id: ${NOTION_DATABASE_ID}

    twitch:
      client-id: ${TWITCH_CLIENT_ID}
      access-token: ${TWITCH_ACCESS_TOKEN}
      refresh-token: ${TWITCH_REFRESH_TOKEN}        # Optional: auto-refresh
      client-secret: ${TWITCH_CLIENT_SECRET}         # Required with refresh-token
      broadcaster-id: "836211004"
      sender-id: "836211004"

    youtube:
      access-token: ${YOUTUBE_ACCESS_TOKEN}
      refresh-token: ${YOUTUBE_REFRESH_TOKEN}        # Optional: auto-refresh
      client-id: ${YOUTUBE_CLIENT_ID}                # Required with refresh-token
      client-secret: ${YOUTUBE_CLIENT_SECRET}         # Required with refresh-token
      channel-id: ${YOUTUBE_CHANNEL_ID}
      live-chat-id: ${YOUTUBE_LIVE_CHAT_ID}

    instagram:
      access-token: ${INSTAGRAM_ACCESS_TOKEN}
      ig-user-id: ${INSTAGRAM_USER_ID}

    webhooks:
      - name: pagerduty
        url: "https://events.pagerduty.com/v2/enqueue"
        method: POST
        headers:
          Authorization: "Token token=${PAGERDUTY_TOKEN}"
        payload-template: '{"routing_key":"{{routingKey}}","event_action":"trigger","payload":{"summary":"{{content}}"}}'

  retry:
    max-attempts: 3                    # Default: 1
    strategy: exponential              # none, fixed, exponential

  scheduling:
    enabled: true
    pool-size: 2

  tracking:
    enabled: true                      # Enable delivery receipts
    type: memory                       # memory (default) or jpa
    dlq-enabled: true                  # Enable dead letter queue

  events:
    enabled: true                      # Spring event publishing

  rate-limit:
    enabled: true                      # Enable rate limiting
    use-defaults: true                 # Use API-aware presets per channel
    max-requests: 100                  # Global default
    window: 60s                        # Global default window
    channels:                          # Per-channel overrides
      slack:
        max-requests: 2
        window: 1s

  deduplication:
    enabled: true
    ttl: 24h
    strategy: content-hash             # content-hash or explicit-key

  audit:
    enabled: true
    type: memory                       # memory (default) or jpa

  audience:
    enabled: true

  status-webhook:
    url: "https://myapp.com/notify/status"
    timeout-ms: 10000
    signing-secret: ${WEBHOOK_SIGNING_SECRET}
    headers:
      X-Custom: value
```

### Injection

```java
@Service
public class MyService {
    private final NotifyHub notify;
    public MyService(NotifyHub notify) { this.notify = notify; }
}
```

Auto-configured channels appear as beans. Only channels with their required properties set are activated (e.g., email requires `notify.channels.email.host`).

---

## MCP Server (AI Agents)

NotifyHub includes a standalone MCP (Model Context Protocol) server with 27 tools for AI agent integration. Run via:

```bash
java -jar notify-mcp/target/notify-mcp-0.9.0.jar
```

### All 27 MCP Tools

**Channel-Specific Sending:**
- `send_email` — Send via SMTP
- `send_sms` — Send via Twilio
- `send_whatsapp` — Send via Twilio WhatsApp
- `send_slack` — Send via webhook
- `send_telegram` — Send via Bot API
- `send_discord` — Send via webhook
- `send_teams` — Send via webhook
- `send_google_chat` — Send via webhook
- `send_push` — Firebase Cloud Messaging
- `send_twitter` — Twitter/X API v2
- `send_linkedin` — LinkedIn REST API
- `send_notion` — Notion Database API
- `send_twitch` — Twitch Helix API chat message
- `send_youtube` — YouTube Live Chat message
- `send_instagram` — Instagram Graph API

**Unified & Batch:**
- `send_notification` — Send via any channel (unified)
- `send_batch` — Same message to multiple recipients
- `send_multi_channel` — Same message across multiple channels
- `send_to_audience` — Send to a named audience

**Contact & Audience Management:**
- `create_contact` — Create contact with tags
- `list_contacts` — List/filter contacts
- `create_audience` — Create audience by tag filter
- `list_audiences` — List audiences

**Monitoring & Analytics:**
- `list_channels` — List configured channels and availability
- `list_delivery_receipts` — View delivery history
- `list_dead_letters` — View failed notifications (DLQ)
- `get_analytics` — Delivery analytics and stats

---

## Key Patterns & Conventions

### Creating a New Channel Module

1. Create `notify-channels/notify-{name}/` with Maven module
2. Implement `NotificationChannel` interface (`getName()`, `isAvailable()`, `send()`)
3. Create `{Name}Config` with builder pattern
4. Add `Notify{Name}AutoConfiguration` in `notify-spring-boot-starter`
5. Add properties to `NotifyProperties.java` inner class
6. Register in `spring.factories` or `@AutoConfiguration`

### Error Handling

- `NotificationSendException` — thrown by channels on send failure
- `OAuthTokenRefreshException` — thrown when token refresh fails
- `RateLimitExceededException` — thrown when rate limit exceeded
- `TemplateNotFoundException` — thrown when template not found

### Thread Safety

- `NotifyHub` is thread-safe and designed for singleton usage
- `OAuthTokenManager` uses `ReentrantReadWriteLock` for concurrent token access
- `TokenBucketRateLimiter` uses `ConcurrentHashMap` for per-channel buckets
- All channel implementations are thread-safe

### Testing

- Tests use JUnit 5 with `@DisplayName` annotations
- Config builder validation tests verify required fields throw `IllegalArgumentException`
- Network-dependent tests use short timeouts and expect exceptions
- Thread safety tests use concurrent threads to verify atomic operations
- Run: `mvn test` (all modules) or `mvn test -pl notify-core` (specific module)

---

## Common Examples

### Send with Fallback Chain

```java
notify.to(user)
    .via(Channel.WHATSAPP).fallback(Channel.SMS).fallback(Channel.EMAIL)
    .template("payment-reminder")
    .param("amount", "R$ 150,00")
    .send();
```

### Multi-Channel Alert

```java
notify.to(user)
    .via(Channel.EMAIL).via(Channel.SLACK).via(Channel.TEAMS)
    .subject("Security Alert")
    .content("Login from new device detected")
    .sendAll();
```

### Batch to All Premium Users

```java
notify.toAudience("premium-users")
    .via(Channel.EMAIL)
    .template("exclusive-offer")
    .param("discount", "30%")
    .send();
```

### Scheduled Notification

```java
notify.to(user)
    .via(Channel.PUSH)
    .content("Don't forget your appointment tomorrow!")
    .schedule(Duration.ofHours(24));
```

### Tracked Async Send

```java
CompletableFuture<Void> future = notify.to(user)
    .via(Channel.EMAIL)
    .template("invoice")
    .param("invoiceId", "INV-456")
    .sendAsync();

future.thenRun(() -> log.info("Invoice email sent"));
```

### YouTube Live Chat + Poll

```java
// Send chat message
notify.to("Cg0KC...liveChatId")
    .via(Channel.YOUTUBE)
    .content("Welcome to the stream!")
    .send();

// YouTube supports live chat messages to active streams
```

### Twitch Chat Message

```java
notify.to("broadcaster-id")
    .via(Channel.TWITCH)
    .content("Hello chat! PogChamp")
    .send();
```

### Instagram DM

```java
notify.to("recipient-user-id")
    .via(Channel.INSTAGRAM)
    .content("Thanks for your order!")
    .send();
```
