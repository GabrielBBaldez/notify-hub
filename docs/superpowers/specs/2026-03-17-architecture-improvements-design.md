# NotifyHub Architecture Improvements — Design Spec

**Date:** 2026-03-17
**Approach:** Inside-Out (core first, expand outward)
**Constraint:** Zero breaking changes. Simplicity for MCP users and library consumers.

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
    private final NotificationExecutor executor;
    private final NotificationScheduler scheduler;
    private final TemplateEngine templateEngine;
    private final AudienceManager audienceManager;
}
```

Builder stays on `NotifyHub` — it constructs `NotificationExecutor` and `NotificationScheduler` internally.

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

Pipeline order: Dedup → RateLimit → CircuitBreaker → Template → Send → Retry → DLQ

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

```java
public interface NotificationChannel {
    // Existing methods unchanged
    default HealthStatus healthCheck() {
        return isAvailable() ? HealthStatus.UP : HealthStatus.DOWN;
    }
}
```

Integrates with Spring Actuator via auto-config.

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
    private final List<NotificationEventListener> listeners;
    void publish(NotificationEvent event) { ... }
}

public interface NotificationEventListener {
    void onEvent(NotificationEvent event);
}
```

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

### 5.2 — Notification Preferences per Recipient

```java
public interface Notifiable {
    // Existing methods unchanged
    default Set<Channel> getOptedOutChannels() { return Set.of(); }
    default QuietHours getQuietHours() { return QuietHours.none(); }
}
```

Pipeline respects automatically: opt-out skips to fallback, quiet hours delays to next allowed time.

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

Uses extracted `NotificationScheduler`.

---

## Cross-Cutting Principles

1. **Zero breaking changes** — all new features are opt-in
2. **Simplicity preserved** — `notify.to().via().send()` never changes
3. **MCP compatibility** — all new features exposed as MCP tools
4. **Default methods** on interfaces for backwards compatibility
5. **Core stays Spring-free** — new classes in core have zero Spring imports
