# CLAUDE.md — NotifyHub

> Unified notification library for Java 17+ and Spring Boot 3.x.
> One fluent API, 17 channels, zero boilerplate.

## Quick Reference

```bash
# Build & test everything
mvn clean verify -B

# Test a specific module
mvn test -pl notify-core
mvn test -pl notify-channels/notify-slack

# Build without tests (fast iteration)
mvn clean compile -DskipTests

# Run demo app (http://localhost:8080)
mvn spring-boot:run -pl notify-demo -DskipTests

# Build MCP server JAR
mvn clean package -pl notify-mcp -am -q -DskipTests

# Run a single test class
mvn test -pl notify-mcp -Dtest="SendEmailToolTest"
```

## Project Identity

- **Group ID**: `io.github.gabrielbbaldez`
- **Version**: `0.9.0`
- **Java**: 17+ (source & target)
- **Spring Boot**: 3.2.5 (optional — core works standalone)
- **License**: MIT
- **Build**: Maven multi-module (23 modules)
- **Repo**: https://github.com/GabrielBBaldez/notify-hub

## Module Map

```
notify-hub/
├── notify-core/                    # Core API — zero Spring dependency
├── notify-channels/                # One module per channel (16 total)
│   ├── notify-email/               #   SMTP (Jakarta Mail)
│   ├── notify-sms/                 #   Twilio SMS
│   ├── notify-slack/               #   Slack webhooks
│   ├── notify-telegram/            #   Telegram Bot API
│   ├── notify-discord/             #   Discord webhooks
│   ├── notify-teams/               #   Microsoft Teams webhooks
│   ├── notify-push-firebase/       #   Firebase Cloud Messaging
│   ├── notify-webhook/             #   Generic HTTP webhooks
│   ├── notify-websocket/           #   Java WebSocket
│   ├── notify-google-chat/         #   Google Chat webhooks
│   ├── notify-twitter/             #   Twitter/X API v2
│   ├── notify-linkedin/            #   LinkedIn REST API
│   ├── notify-notion/              #   Notion Database API
│   ├── notify-twitch/              #   Twitch Helix API
│   ├── notify-youtube/             #   YouTube Live Chat API
│   └── notify-instagram/           #   Instagram Graph API
├── notify-spring-boot-starter/     # Auto-configuration + properties
├── notify-tracker-jpa/             # JPA delivery tracking (optional)
├── notify-audit-jpa/               # JPA audit logging (optional)
├── notify-admin/                   # Web dashboard (Thymeleaf)
├── notify-queue-rabbitmq/          # RabbitMQ integration (optional)
├── notify-queue-kafka/             # Kafka integration (optional)
├── notify-demo/                    # Demo Spring Boot app
├── notify-mcp/                     # MCP Server for AI agents (27 tools)
└── docs/                           # Static landing page (GitHub Pages)
```

## Architecture & Key Classes

### Core (notify-core)

| Class | Role |
|---|---|
| `NotifyHub` | Main entry point. Singleton facade. Builder pattern. |
| `NotificationBuilder` | Fluent API chain (`.to().via().content().send()`) |
| `Notification` | Immutable notification data object |
| `NotificationChannel` | Interface all channels implement (`getName`, `send`, `isAvailable`) |
| `Channel` | Enum: EMAIL, SMS, WHATSAPP, SLACK, TELEGRAM, DISCORD, TEAMS, PUSH, WEBSOCKET, GOOGLE_CHAT, TWITTER, LINKEDIN, NOTION, TWITCH, YOUTUBE, INSTAGRAM |
| `Priority` | Enum: URGENT (bypasses rate limits), HIGH, NORMAL, LOW |
| `DeliveryReceipt` | Immutable tracking receipt (id, status, timestamp) |
| `DeliveryStatus` | Enum: PENDING, SCHEDULED, SENT, FAILED, CANCELLED |
| `Notifiable` | Interface for domain objects (`getNotifyEmail`, `getNotifyPhone`, `getPreferredChannels`) |
| `NotificationListener` | Observer: `onSuccess`, `onFailure`, `onScheduled`, `onCancelled` |
| `TemplateEngine` | Interface for template rendering (impl: `MustacheTemplateEngine`) |
| `RetryPolicy` | Strategy: `none()`, `fixed(n, delay)`, `exponential(n)` |
| `RateLimitConfig` | Per-channel presets: `youtube()`, `twitch()`, `slack()`, etc. |
| `TokenBucketRateLimiter` | Rate limiter impl with `ConcurrentHashMap` |
| `OAuthTokenManager` | Thread-safe OAuth2 refresh (`ReentrantReadWriteLock`) |
| `AudienceManager` | Contact & audience management with tag-based segmentation |
| `DeadLetterQueue` | Failed notification capture after retry exhaustion |

### Package Structure

```
io.notifyhub.core           # Core API
io.notifyhub.channel.{name} # Each channel (email, slack, telegram, etc.)
io.notifyhub.spring         # Spring Boot auto-configuration
io.notifyhub.mcp            # MCP server
io.notifyhub.admin          # Admin dashboard
io.notifyhub.queue.{type}   # Queue integrations (rabbitmq, kafka)
```

## Code Conventions

### Naming Patterns

- Channel classes: `{Name}Channel` (e.g., `SmtpEmailChannel`, `SlackChannel`, `TelegramChannel`)
- Config classes: `{Name}Config` with builder pattern (e.g., `SmtpConfig.builder()`)
- Spring auto-configs: `Notify{Name}AutoConfiguration`
- Spring properties: inner classes in `NotifyProperties`
- Exceptions: `NotificationSendException`, `OAuthTokenRefreshException`, `RateLimitExceededException`
- Test classes: `{Name}Test`, `{Name}IntegrationTest`

### Design Patterns Used

- **Builder** — All configs, `NotifyHub`, `DeliveryReceipt`, `Contact`
- **Fluent API** — `NotificationBuilder` chain
- **Strategy** — `RetryPolicy`, `RateLimiter`, `TemplateEngine`
- **Observer** — `NotificationListener`
- **Adapter** — Each `NotificationChannel` adapts a different API
- **Immutability** — `Notification`, `DeliveryReceipt`, configs use `final` fields + `Collections.unmodifiable*()`

### Thread Safety

- `NotifyHub` is thread-safe, designed for singleton usage
- `OAuthTokenManager` uses `ReentrantReadWriteLock`
- `TokenBucketRateLimiter` uses `ConcurrentHashMap`
- All channel implementations are stateless and thread-safe

### Code Style

- Java 17 features OK (records, sealed classes, text blocks, pattern matching)
- No Lombok — use manual builders
- SLF4J for logging: `private static final Logger log = LoggerFactory.getLogger(Foo.class)`
- Log DEBUG on success, WARN/ERROR on failure
- Channels use JDK 11+ `HttpClient` (no external HTTP libs except Twilio SDK)
- Collections returned as unmodifiable
- `@DisplayName` on all test methods

## How to Create a New Channel

Follow this exact recipe:

1. **Create module** `notify-channels/notify-{name}/` with `pom.xml` depending on `notify-core`
2. **Config class** `{Name}Config` with builder pattern:
   ```java
   public class PagerDutyConfig {
       private final String apiKey;
       // ... builder(), getters, validation in build()
   }
   ```
3. **Channel class** implementing `NotificationChannel`:
   ```java
   public class PagerDutyChannel implements NotificationChannel {
       public String getName() { return "pagerduty"; }
       public boolean isAvailable() { return config.getApiKey() != null; }
       public void send(Notification notification) throws NotificationSendException { ... }
   }
   ```
4. **Add module** to root `pom.xml` `<modules>` section
5. **Spring auto-config** in `notify-spring-boot-starter`:
   - Add `Notify{Name}AutoConfiguration` class
   - Add properties to `NotifyProperties`
   - Register in `NotifyAutoConfiguration` imports
6. **Tests**: unit test with Mockito + integration test if applicable
7. **MCP tool** (optional): add `Send{Name}Tool` in `notify-mcp`

## Spring Boot Auto-Configuration

### How It Works

- Main: `NotifyAutoConfiguration` imports per-channel auto-configs
- Each channel activates via `@ConditionalOnProperty(prefix = "notify.channels.{name}")`
- Features activate via properties: `notify.tracking.enabled`, `notify.rate-limit.enabled`, etc.
- Optional integrations (Micrometer, OpenTelemetry) use `@ConditionalOnClass` in nested configs

### Property Prefix

All properties under `notify.*`:
- `notify.channels.email.*` — SMTP config
- `notify.channels.sms.*` — Twilio SMS
- `notify.channels.slack.*` — Slack webhook
- `notify.retry.*` — Retry policy
- `notify.rate-limit.*` — Rate limiting
- `notify.tracking.*` — Delivery tracking
- `notify.deduplication.*` — Dedup settings
- `notify.audit.*` — Audit logging
- `notify.audience.*` — Contact/audience management
- `notify.scheduling.*` — Scheduled notifications

## Testing

### Stack

- JUnit 5 (5.10.2)
- Mockito (5.11.0) with `@ExtendWith(MockitoExtension.class)`
- GreenMail for SMTP integration tests

### Patterns

```java
@ExtendWith(MockitoExtension.class)
class MyChannelTest {
    @Mock private SomeDependency dep;
    private MyChannel channel;

    @BeforeEach
    void setUp() { channel = new MyChannel(config); }

    @Test
    @DisplayName("Should send notification successfully")
    void testSend() { ... }
}
```

### Running Tests

```bash
mvn test                                    # All modules
mvn test -pl notify-core                    # Core only
mvn test -pl notify-channels/notify-slack   # Specific channel
mvn test -pl notify-mcp -Dtest="SendEmailToolTest"  # Single class
```

## CI/CD

### GitHub Actions

- **CI** (`ci.yml`): runs on push/PR to `master`, matrix Java 17 + 21, `mvn clean verify -B`
- **Release** (`release.yml`): triggered by `v*` tags, deploys to Maven Central with GPG signing

### Release Process

1. Update version in all `pom.xml` files
2. Commit and tag: `git tag v0.9.0`
3. Push tag: `git push origin v0.9.0`
4. GitHub Actions builds, signs, publishes to Maven Central

## MCP Server (notify-mcp)

- 27 tools for AI agent integration
- STDIO transport (JSON-RPC)
- Built as fat JAR: `java -jar notify-mcp-0.9.0.jar`
- Each tool is a class in `io.notifyhub.mcp.tools/`
- Config via environment variables (see `.mcp.json`)

## Dev Servers (Claude Code)

Defined in `.claude/launch.json`:
- `notifyhub-demo` — Spring Boot demo app on port 8080
- `landing-page` — Static docs site on port 8091

## Landing Page

- Location: `docs/` (static HTML, served via GitHub Pages)
- Files: `index.html`, `docs.html`, `style.css`, `docs.css`, `logo.png`
- Icons from `cdn.simpleicons.org`
- Badges from `shields.io`

## Common Gotchas

- `notify-core` has ZERO Spring dependency — don't add Spring imports there
- Channel modules depend only on `notify-core` — no cross-channel dependencies
- Spring auto-config classes must use nested `@Configuration` for optional deps (Micrometer, OTel) to avoid `ClassNotFoundException`
- WhatsApp reuses Twilio SDK from `notify-sms` module
- `Priority.URGENT` bypasses rate limiting — this is intentional
- Template path convention: `src/main/resources/templates/notify/{template-name}/{variant}.mustache`
- The demo app uses embedded GreenMail — no real SMTP server needed
- MCP server JAR version in `.mcp.json` may lag behind `pom.xml` version — keep in sync
