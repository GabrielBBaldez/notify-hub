# NotifyHub Architecture Improvements — Design Spec

**Date:** 2026-03-17
**Approach:** Inside-Out (core first, expand outward)
**Constraint:** Minimal breaking changes (internal only). Simplicity for MCP users and library consumers.

---

## Seção 1: Refatoração dos God Objects

### 1.1 — NotifyHub.java (836 → ~200 linhas)

Extract 3 internal classes from `NotifyHub`:

| Current Responsibility | Lines | Extract To |
|---|---|---|
| Execution engine (`execute`, `executeAll`, `sendToChannel`, `sendWithRetry`) | ~180 | `NotificationExecutor` |
| Tracked execution (`executeTracked`, `executeAllTracked`) | ~110 | `NotificationExecutor` |
| Scheduled execution (`executeScheduled`, `getScheduler`) | ~90 | `NotificationScheduler` |
| Listener notification (`notifyListeners`, `notifyListenersSuccess`) | ~30 | `ListenerNotifier` (package-private) |

`NotifyHub` stays as thin facade with:
- Fluent API entry points (`to`, `toPhone`, `toAll`, `toAudience`, `notify`)
- Channel management (`registerChannel`, `getChannel`, `getRegisteredChannels`)
- Delegation to extracted classes

**Public API does not change.** `NotificationExecutor` encapsulates the pipeline:
```
sendToChannel → dedup check → rate limit → template render → build notification → sendWithRetry → dedup mark → DLQ on failure
```

Fields in `NotifyHub` after refactoring:
```java
public class NotifyHub {
    private final Map<String, NotificationChannel> channels;
    private final NotificationExecutor executor;      // owns: rateLimiter, deduplicationStore, deadLetterQueue, templateEngine, router
    private final NotificationScheduler scheduler;     // owns: ScheduledExecutorService
    private final NotificationEventBus eventBus;       // owns: listeners (legacy + new)
    private final AudienceManager audienceManager;
}
```

**Dependency flow (no circular refs):**
- `NotificationExecutor` receives `NotificationEventBus` to publish events
- `NotificationScheduler` receives `NotificationExecutor` (to execute) and `NotificationEventBus` (to publish scheduled/cancelled events)
- `NotificationEventBus` has no dependency on executor or scheduler

Builder stays on `NotifyHub` — it constructs `NotificationEventBus`, `NotificationExecutor`, and `NotificationScheduler` internally in that order.

### 1.2 — NotifyProperties.java (590 → ~80 lines + separate files)

Extract each channel inner class to its own file in `io.notifyhub.spring.properties`:

```
NotifyProperties.java          (~80 lines, top-level fields only)
├── properties/
│   ├── EmailProperties.java
│   ├── SmsProperties.java
│   ├── SlackProperties.java
│   └── ... (1 per channel)
```

**Breaking change note:** Inner class types like `NotifyProperties.Email` change to `EmailProperties`.
The `Channels` class still holds fields of the extracted types, so `props.getChannels().getEmail()`
continues to work — only direct type references to inner classes break. This is acceptable as
inner class types are not part of the documented public API. Add `@Deprecated` type aliases
in the first release for a smooth transition if needed.

### 1.3 — DemoController.java (815 → multiple controllers)

Split into per-channel demo controllers (~100 lines each):
- `DemoEmailController`
- `DemoSmsController`
- `DemoBatchController`
- `DemoSchedulingController`
- etc.

---

## Seção 2: Pipeline de Resiliência

### 2.1 — Pipeline as handler chain

```java
interface SendHandler {
    SendResult handle(SendContext context, SendHandler next);
}
```

Pipeline order: Dedup → RateLimit → CircuitBreaker → Template → Retry(Send) → DLQ

**Note:** Retry wraps Send as a decorator, not a sequential step. The `RetrySendHandler`
invokes the inner `ChannelSendHandler` up to N times with backoff. This preserves the
current `sendWithRetry()` semantics where retry re-invokes `channel.sendWithResult()`.

Assembled internally by `NotificationExecutor`. Developer does not need to know about the pipeline — same fluent API.

Optional configuration:
```java
NotifyHub.builder()
    .circuitBreaker(CircuitBreakerConfig.defaults())
    .bulkhead(BulkheadConfig.perChannel(10))
    .build();
```

### 2.2 — Circuit Breaker per channel

```java
public class ChannelCircuitBreaker {
    // States: CLOSED (normal), OPEN (blocked), HALF_OPEN (testing)
    private final int failureThreshold;      // default: 5
    private final Duration openDuration;      // default: 30s
    private final Duration windowSize;        // default: 60s
}
```

When circuit opens, fallback channels are tried automatically.

### 2.3 — Bulkhead Isolation

Each channel runs in an isolated pool:

```java
public class BulkheadConfig {
    private final int maxConcurrentCalls;  // default: 10 per channel
    public static BulkheadConfig perChannel(int max) { ... }
    public static BulkheadConfig defaults() { ... }
}
```

### 2.4 — Health Check

Health checks stay in the Spring layer only (core stays Spring-free). The existing
`NotificationChannel.isAvailable()` is sufficient for core.

In `notify-spring-boot-starter`, enhance the existing `NotifyHubHealthIndicator` to:
- Report per-channel health (UP/DOWN based on `isAvailable()`)
- Include circuit breaker state when configured
- Expose as Spring Actuator endpoint automatically

```java
// In notify-spring-boot-starter (NOT in core)
@Component
@ConditionalOnClass(HealthIndicator.class)
public class NotifyHubHealthIndicator implements HealthIndicator {
    // Iterates channels, calls isAvailable(), reports per-channel status
    // Also reports circuit breaker state if configured
}
```

No changes to `NotificationChannel` interface.

---

## Seção 3: Observabilidade Unificada

### 3.1 — NotificationEvent as common currency

```java
public record NotificationEvent(
    String id,
    EventType type,       // SENT, FAILED, RETRIED, RATE_LIMITED, DEDUPED, CIRCUIT_OPENED, SCHEDULED, CANCELLED
    String channelName,
    String recipient,
    String templateName,
    Duration latency,
    String errorMessage,
    Instant timestamp,
    Map<String, String> metadata
) {}
```

### 3.2 — Internal EventBus

```java
class NotificationEventBus {
    // Thread-safe: listeners are set at construction time (immutable after build)
    // CopyOnWriteArrayList used if runtime listener addition is needed
    private final List<NotificationEventListener> listeners;

    void publish(NotificationEvent event) {
        // Safe for concurrent calls from multiple threads
        for (var listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                // Log and continue — one listener failure doesn't block others
            }
        }
    }
}

public interface NotificationEventListener {
    void onEvent(NotificationEvent event);
}
```

**Thread-safety:** The `NotificationEventBus` is constructed once during `NotifyHub.build()` with
all listeners. The list is wrapped as `Collections.unmodifiableList()`. If runtime listener
addition is needed (unlikely), switch to `CopyOnWriteArrayList`. All `publish()` calls are
safe for concurrent invocation from `sendToChannel`, `executeScheduled`, and async paths.

Consumers:
- `NotificationTracker` — records `DeliveryReceipt`
- `AuditLog` — records audit trail
- `MetricsEventListener` — emits Micrometer counters/timers
- `LegacyListenerAdapter` — converts to existing `NotificationListener.onSuccess/onFailure`

### 3.3 — Automatic metrics per channel

```
notifyhub.send.total{channel, status}          → Counter
notifyhub.send.latency{channel}                → Timer
notifyhub.retry.total{channel}                 → Counter
notifyhub.circuit_breaker.state{channel, state} → Gauge
notifyhub.rate_limit.rejected{channel}          → Counter
notifyhub.dlq.size                              → Gauge
```

Activated via `@ConditionalOnClass(MeterRegistry.class)`.

### 3.4 — Legacy NotificationListener preserved

`LegacyListenerAdapter` converts EventBus events to the existing listener interface.

---

## Seção 4: Limpeza dos Clusters Orfãos + DX

### 4.1 — Cluster resolution

| Cluster | Resolution |
|---|---|
| Cluster_82 (execution methods) | Resolved by Seção 1 — extraction to NotificationExecutor |
| Cluster_86 (tracker) | Resolved by Seção 3 — unified EventBus |
| Cluster_87 (listener + audit) | Resolved by Seção 3 — NotificationEvent |
| Cluster_91 (attachments) | Move to `io.notifyhub.core.attachment` package |
| Cluster_94 (audit log) | Resolved by Seção 3 — unified EventBus |
| Cluster_97 (builder methods) | Normal — fluent API, no action needed |
| Cluster_72 (webhook signing tests) | Move to `notify-webhook/src/test/` |

### 4.2 — Channel template/archetype

```
notify-channels/notify-template/
├── src/main/java/.../TemplateChannel.java
├── src/main/java/.../TemplateConfig.java
├── src/test/java/.../TemplateChannelTest.java
└── pom.xml
```

Copy module, find-replace "Template" with channel name, implement `send()`.

### 4.3 — Better error messages

- Suggest closest channel name (Levenshtein distance)
- Show config property name and env var for missing config

### 4.4 — TestNotifyHub for library consumers

```java
var testHub = TestNotifyHub.create();
testHub.to("user@test.com").via(EMAIL).content("Hello").send();
assertThat(testHub.sent()).hasSize(1);
```

---

## Seção 5: Features Novas

### 5.1 — Multi-Channel Orchestration

```java
notify.to(user)
    .orchestrate()
    .first(EMAIL).template("promo")
    .ifNoOpen(Duration.ofHours(24))
    .then(PUSH).content("Check your email!")
    .execute();
```

New `OrchestrationBuilder` — does not pollute existing `NotificationBuilder`.
Requires `NotificationTracker` to check open status.

**Fallback for channels that don't report opens:** Most channels (Slack, Telegram, SMS, Push, etc.)
never fire an OPENED webhook. For these channels, `ifNoOpen(duration)` behaves as `ifNoDeliveryConfirmation(duration)`:
- If the channel reports DELIVERED → consider "opened" (skip escalation)
- If the channel reports nothing after the timeout → escalate to next step
- If the channel reports FAILED → escalate immediately

This means orchestration works across all 24 channels, not just email providers with open tracking.
The `OrchestrationBuilder` will document this behavior clearly in javadoc.

### 5.2 — Notification Preferences per Recipient

```java
public interface Notifiable {
    // Existing methods unchanged
    default Set<Channel> getOptedOutChannels() { return Set.of(); }
    default QuietHours getQuietHours() { return QuietHours.none(); }
}

/**
 * Quiet hours configuration. Respects recipient's timezone.
 * During quiet hours, notifications are queued and delivered at the next allowed time.
 */
public class QuietHours {
    private final LocalTime start;      // e.g., 22:00
    private final LocalTime end;        // e.g., 08:00
    private final ZoneId timezone;      // e.g., America/Sao_Paulo

    public static QuietHours none() { return NONE; }
    public static QuietHours between(LocalTime start, LocalTime end, ZoneId tz) { ... }

    /** Returns the next Instant when delivery is allowed, or now if not in quiet period. */
    public Instant nextAllowedTime(Instant now) { ... }
}
```

Pipeline respects automatically:
- **Opt-out:** If the channel is in `getOptedOutChannels()`, skip to fallback channel. If no fallback, skip silently (no error).
- **Quiet hours:** If current time is within quiet hours, the notification is scheduled via `NotificationScheduler` for `nextAllowedTime()`. Uses the recipient's timezone. Returns a `ScheduledNotification` instead of sending immediately.

### 5.3 — A/B Testing

```java
notify.to(user).via(EMAIL)
    .abTest("welcome-experiment")
        .variant("A", b -> b.template("welcome-v1"))
        .variant("B", b -> b.template("welcome-v2"))
        .split(50, 50)
    .send();
```

Deterministic hash of recipient for consistent variant assignment.
Results in `NotificationEvent` metadata + Micrometer counters.

### 5.4 — Cron Scheduling

```java
notify.to("team@company.com").via(EMAIL)
    .template("daily-report")
    .cron("0 9 * * MON-FRI")
    .send();
```

Uses extracted `NotificationScheduler`. Cron parsing via a lightweight internal parser
supporting standard 5-field expressions (minute, hour, day-of-month, month, day-of-week).
No external dependency needed — the parser only needs to compute "next fire time" from
a cron expression, which is ~100 lines of code.

---

## Cross-Cutting Principles

1. **Minimal breaking changes** — all new features are opt-in; only internal type references (e.g., `NotifyProperties.Email` → `EmailProperties`) may change; public fluent API is untouched
2. **Simplicity preserved** — `notify.to().via().send()` never changes
3. **MCP compatibility** — all new features exposed as MCP tools
4. **Default methods** on interfaces for backwards compatibility
5. **Core stays Spring-free** — new classes in core have zero Spring imports
