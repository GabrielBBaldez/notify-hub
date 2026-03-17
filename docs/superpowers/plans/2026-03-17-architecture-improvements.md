# NotifyHub Architecture Improvements — Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor NotifyHub's god objects, add resilience pipeline, unified observability, better DX, and new features — all without breaking the public fluent API.

**Architecture:** Inside-out approach. Start with core extraction (NotificationEventBus → NotificationExecutor → NotificationScheduler), then layer resilience handlers, observability, DX improvements, and new features on top. Each chunk produces working, testable software.

**Tech Stack:** Java 17, JUnit 5, Mockito 5, SLF4J, optional Spring Boot 3.2.5, optional Micrometer

**Spec:** `docs/superpowers/specs/2026-03-17-architecture-improvements-design.md`

---

## Chunk 1: Core Refactoring — Extract God Objects

### Task 1: Create NotificationEventBus and EventType

The EventBus must exist first because both NotificationExecutor and NotificationScheduler depend on it.

**Files:**
- Create: `notify-core/src/main/java/io/notifyhub/core/event/EventType.java`
- Create: `notify-core/src/main/java/io/notifyhub/core/event/NotificationEvent.java`
- Create: `notify-core/src/main/java/io/notifyhub/core/event/NotificationEventListener.java`
- Create: `notify-core/src/main/java/io/notifyhub/core/event/NotificationEventBus.java`
- Create: `notify-core/src/test/java/io/notifyhub/core/event/NotificationEventBusTest.java`

- [ ] **Step 1: Write EventType enum**

```java
// notify-core/src/main/java/io/notifyhub/core/event/EventType.java
package io.notifyhub.core.event;

public enum EventType {
    SENT, FAILED, RETRIED, RATE_LIMITED, DEDUPED,
    CIRCUIT_OPENED, CIRCUIT_CLOSED, CIRCUIT_HALF_OPEN,
    SCHEDULED, CANCELLED
}
```

- [ ] **Step 2: Write NotificationEvent record**

```java
// notify-core/src/main/java/io/notifyhub/core/event/NotificationEvent.java
package io.notifyhub.core.event;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public record NotificationEvent(
    String id,
    EventType type,
    String channelName,
    String recipient,
    String templateName,
    Duration latency,
    String errorMessage,
    Instant timestamp,
    Map<String, String> metadata
) {
    public NotificationEvent {
        if (type == null) throw new IllegalArgumentException("type must not be null");
        if (channelName == null) throw new IllegalArgumentException("channelName must not be null");
        timestamp = timestamp != null ? timestamp : Instant.now();
        metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private EventType type;
        private String channelName;
        private String recipient;
        private String templateName;
        private Duration latency;
        private String errorMessage;
        private Instant timestamp;
        private Map<String, String> metadata;

        public Builder id(String id) { this.id = id; return this; }
        public Builder type(EventType type) { this.type = type; return this; }
        public Builder channelName(String channelName) { this.channelName = channelName; return this; }
        public Builder recipient(String recipient) { this.recipient = recipient; return this; }
        public Builder templateName(String templateName) { this.templateName = templateName; return this; }
        public Builder latency(Duration latency) { this.latency = latency; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public Builder metadata(Map<String, String> metadata) { this.metadata = metadata; return this; }

        public NotificationEvent build() {
            return new NotificationEvent(id, type, channelName, recipient, templateName,
                    latency, errorMessage, timestamp, metadata);
        }
    }
}
```

- [ ] **Step 3: Write NotificationEventListener interface**

```java
// notify-core/src/main/java/io/notifyhub/core/event/NotificationEventListener.java
package io.notifyhub.core.event;

public interface NotificationEventListener {
    void onEvent(NotificationEvent event);
}
```

- [ ] **Step 4: Write failing tests for NotificationEventBus**

```java
// notify-core/src/test/java/io/notifyhub/core/event/NotificationEventBusTest.java
package io.notifyhub.core.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class NotificationEventBusTest {

    @Test
    @DisplayName("Should publish event to all listeners")
    void publishToAllListeners() {
        List<NotificationEvent> received1 = new ArrayList<>();
        List<NotificationEvent> received2 = new ArrayList<>();

        NotificationEventBus bus = new NotificationEventBus(List.of(
            received1::add, received2::add
        ));

        NotificationEvent event = NotificationEvent.builder()
            .type(EventType.SENT).channelName("email").recipient("a@test.com").build();
        bus.publish(event);

        assertEquals(1, received1.size());
        assertEquals(1, received2.size());
        assertEquals("email", received1.get(0).channelName());
    }

    @Test
    @DisplayName("Should not fail when listener throws exception")
    void listenerExceptionDoesNotBreakOthers() {
        List<NotificationEvent> received = new ArrayList<>();

        NotificationEventBus bus = new NotificationEventBus(List.of(
            e -> { throw new RuntimeException("boom"); },
            received::add
        ));

        NotificationEvent event = NotificationEvent.builder()
            .type(EventType.SENT).channelName("slack").recipient("ch").build();

        assertDoesNotThrow(() -> bus.publish(event));
        assertEquals(1, received.size());
    }

    @Test
    @DisplayName("Should work with empty listener list")
    void emptyListeners() {
        NotificationEventBus bus = new NotificationEventBus(List.of());
        NotificationEvent event = NotificationEvent.builder()
            .type(EventType.SENT).channelName("email").recipient("a@test.com").build();
        assertDoesNotThrow(() -> bus.publish(event));
    }
}
```

- [ ] **Step 5: Run tests to verify they fail**

Run: `mvn test -pl notify-core -Dtest="NotificationEventBusTest" -B`
Expected: FAIL — `NotificationEventBus` class doesn't exist yet

- [ ] **Step 6: Write NotificationEventBus implementation**

```java
// notify-core/src/main/java/io/notifyhub/core/event/NotificationEventBus.java
package io.notifyhub.core.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collections;
import java.util.List;

public class NotificationEventBus {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventBus.class);
    private final List<NotificationEventListener> listeners;

    public NotificationEventBus(List<NotificationEventListener> listeners) {
        this.listeners = Collections.unmodifiableList(listeners);
    }

    public void publish(NotificationEvent event) {
        for (NotificationEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                log.warn("Event listener failed for event type {}: {}",
                    event.type(), e.getMessage());
            }
        }
    }
}
```

- [ ] **Step 7: Run tests to verify they pass**

Run: `mvn test -pl notify-core -Dtest="NotificationEventBusTest" -B`
Expected: PASS — all 3 tests green

- [ ] **Step 8: Commit**

```bash
git add notify-core/src/main/java/io/notifyhub/core/event/
git add notify-core/src/test/java/io/notifyhub/core/event/
git commit -m "feat(core): add NotificationEventBus, NotificationEvent, EventType"
```

---

### Task 2: Create LegacyListenerAdapter

Bridges the new EventBus to the existing `NotificationListener` interface.

**Files:**
- Create: `notify-core/src/main/java/io/notifyhub/core/event/LegacyListenerAdapter.java`
- Create: `notify-core/src/test/java/io/notifyhub/core/event/LegacyListenerAdapterTest.java`

- [ ] **Step 1: Write failing tests**

```java
// notify-core/src/test/java/io/notifyhub/core/event/LegacyListenerAdapterTest.java
package io.notifyhub.core.event;

import io.notifyhub.core.NotificationListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LegacyListenerAdapterTest {

    @Mock
    private NotificationListener legacy;

    @Test
    @DisplayName("Should forward SENT event to onSuccess")
    void sentMapsToOnSuccess() {
        LegacyListenerAdapter adapter = new LegacyListenerAdapter(legacy);
        NotificationEvent event = NotificationEvent.builder()
            .type(EventType.SENT).channelName("email").templateName("welcome").build();

        adapter.onEvent(event);

        verify(legacy).onSuccess("email", "welcome");
    }

    @Test
    @DisplayName("Should forward FAILED event to onFailure")
    void failedMapsToOnFailure() {
        LegacyListenerAdapter adapter = new LegacyListenerAdapter(legacy);
        NotificationEvent event = NotificationEvent.builder()
            .type(EventType.FAILED).channelName("sms").templateName("otp")
            .errorMessage("timeout").build();

        adapter.onEvent(event);

        verify(legacy).onFailure(eq("sms"), eq("otp"), any(Exception.class));
    }

    @Test
    @DisplayName("Should forward SCHEDULED event to onScheduled")
    void scheduledMapsToOnScheduled() {
        LegacyListenerAdapter adapter = new LegacyListenerAdapter(legacy);
        NotificationEvent event = NotificationEvent.builder()
            .type(EventType.SCHEDULED).channelName("email").recipient("user@test.com")
            .latency(Duration.ofMinutes(30)).build();

        adapter.onEvent(event);

        verify(legacy).onScheduled("email", "user@test.com", Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("Should forward CANCELLED event to onCancelled")
    void cancelledMapsToOnCancelled() {
        LegacyListenerAdapter adapter = new LegacyListenerAdapter(legacy);
        NotificationEvent event = NotificationEvent.builder()
            .type(EventType.CANCELLED).channelName("email").recipient("user@test.com").build();

        adapter.onEvent(event);

        verify(legacy).onCancelled("email", "user@test.com");
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn test -pl notify-core -Dtest="LegacyListenerAdapterTest" -B`
Expected: FAIL

- [ ] **Step 3: Write LegacyListenerAdapter**

```java
// notify-core/src/main/java/io/notifyhub/core/event/LegacyListenerAdapter.java
package io.notifyhub.core.event;

import io.notifyhub.core.NotificationListener;
import io.notifyhub.core.channel.NotificationSendException;

import java.time.Duration;

public class LegacyListenerAdapter implements NotificationEventListener {

    private final NotificationListener delegate;

    public LegacyListenerAdapter(NotificationListener delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onEvent(NotificationEvent event) {
        switch (event.type()) {
            case SENT -> delegate.onSuccess(event.channelName(), event.templateName());
            case FAILED -> delegate.onFailure(event.channelName(), event.templateName(),
                    new NotificationSendException(event.channelName(),
                        event.errorMessage() != null ? event.errorMessage() : "Unknown error"));
            case SCHEDULED -> delegate.onScheduled(event.channelName(), event.recipient(),
                    event.latency() != null ? event.latency() : Duration.ZERO);
            case CANCELLED -> delegate.onCancelled(event.channelName(), event.recipient());
            default -> { /* Other event types have no legacy equivalent */ }
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn test -pl notify-core -Dtest="LegacyListenerAdapterTest" -B`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add notify-core/src/main/java/io/notifyhub/core/event/LegacyListenerAdapter.java
git add notify-core/src/test/java/io/notifyhub/core/event/LegacyListenerAdapterTest.java
git commit -m "feat(core): add LegacyListenerAdapter bridging EventBus to NotificationListener"
```

---

### Task 3: Extract NotificationExecutor from NotifyHub

This is the biggest extraction. Move `execute`, `executeAll`, `executeAsync`, `executeAllAsync`, `executeTracked`, `executeAllTracked`, `sendToChannel`, `sendWithRetry` and all listener-notifying logic.

**Files:**
- Create: `notify-core/src/main/java/io/notifyhub/core/NotificationExecutor.java`
- Create: `notify-core/src/test/java/io/notifyhub/core/NotificationExecutorTest.java`
- Modify: `notify-core/src/main/java/io/notifyhub/core/NotifyHub.java`

- [ ] **Step 1: Write failing tests for NotificationExecutor**

Test the core pipeline: send, fallback, retry, dedup, rate-limit, DLQ, tracked, async.

```java
// notify-core/src/test/java/io/notifyhub/core/NotificationExecutorTest.java
package io.notifyhub.core;

import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
import io.notifyhub.core.dedup.InMemoryDeduplicationStore;
import io.notifyhub.core.dlq.DeadLetterQueue;
import io.notifyhub.core.dlq.InMemoryDeadLetterQueue;
import io.notifyhub.core.event.EventType;
import io.notifyhub.core.event.NotificationEvent;
import io.notifyhub.core.event.NotificationEventBus;
import io.notifyhub.core.event.NotificationEventListener;
import io.notifyhub.core.ratelimit.RateLimiter;
import io.notifyhub.core.retry.RetryPolicy;
import io.notifyhub.core.template.TemplateEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationExecutorTest {

    @Mock private NotificationChannel emailChannel;
    @Mock private NotificationChannel smsChannel;
    @Mock private TemplateEngine templateEngine;
    @Mock private RateLimiter rateLimiter;

    private List<NotificationEvent> publishedEvents;
    private NotificationEventBus eventBus;
    private Map<String, NotificationChannel> channels;

    @BeforeEach
    void setUp() {
        when(emailChannel.getName()).thenReturn("email");
        when(smsChannel.getName()).thenReturn("sms");
        when(emailChannel.sendWithResult(any())).thenCallRealMethod();
        when(smsChannel.sendWithResult(any())).thenCallRealMethod();
        when(rateLimiter.tryAcquire(any())).thenReturn(true);

        publishedEvents = new ArrayList<>();
        eventBus = new NotificationEventBus(List.of(publishedEvents::add));
        channels = new ConcurrentHashMap<>();
        channels.put("email", emailChannel);
        channels.put("sms", smsChannel);
    }

    private NotificationExecutor buildExecutor() {
        return new NotificationExecutor(
            channels, templateEngine, RetryPolicy.none(), eventBus,
            null, rateLimiter, null, null
        );
    }

    @Test
    @DisplayName("Should send to channel and publish SENT event")
    void sendPublishesSentEvent() {
        NotificationExecutor executor = buildExecutor();
        NotifyHub hub = NotifyHub.builder()
            .channel(emailChannel).channel(smsChannel).build();

        NotificationBuilder builder = new NotificationBuilder(hub)
            .to("user@test.com").via(Channel.EMAIL).content("Hello");

        executor.execute(builder);

        verify(emailChannel).send(any());
        assertTrue(publishedEvents.stream().anyMatch(e -> e.type() == EventType.SENT));
    }

    @Test
    @DisplayName("Should try fallback when primary channel fails")
    void fallbackOnFailure() {
        doThrow(new NotificationSendException("email", "down")).when(emailChannel).send(any());

        NotificationExecutor executor = buildExecutor();
        NotifyHub hub = NotifyHub.builder()
            .channel(emailChannel).channel(smsChannel).build();

        NotificationBuilder builder = new NotificationBuilder(hub)
            .to("user@test.com").via(Channel.EMAIL).fallback(Channel.SMS).content("Hello");
        builder.toPhone("+5511999999999");

        executor.execute(builder);

        verify(smsChannel).send(any());
    }

    @Test
    @DisplayName("Should send to DLQ after retry exhaustion")
    void dlqAfterRetryExhaustion() {
        doThrow(new NotificationSendException("email", "down")).when(emailChannel).send(any());
        InMemoryDeadLetterQueue dlq = new InMemoryDeadLetterQueue();

        NotificationExecutor executor = new NotificationExecutor(
            channels, templateEngine, RetryPolicy.fixed(2, java.time.Duration.ofMillis(1)),
            eventBus, null, rateLimiter, dlq, null
        );

        NotifyHub hub = NotifyHub.builder().channel(emailChannel).build();
        NotificationBuilder builder = new NotificationBuilder(hub)
            .to("user@test.com").via(Channel.EMAIL).content("Hello");

        assertThrows(NotificationSendException.class, () -> executor.execute(builder));
        assertEquals(1, dlq.getAll().size());
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn test -pl notify-core -Dtest="NotificationExecutorTest" -B`
Expected: FAIL — `NotificationExecutor` class doesn't exist

- [ ] **Step 3: Write NotificationExecutor — extract from NotifyHub lines 248-697**

Move the following methods from `NotifyHub` into `NotificationExecutor`:
- `execute(NotificationBuilder)` (line 254)
- `executeAll(NotificationBuilder)` (line 284)
- `executeAsync(NotificationBuilder)` (line 308)
- `executeAllAsync(NotificationBuilder)` (line 316)
- `executeTracked(NotificationBuilder)` (line 326)
- `executeAllTracked(NotificationBuilder)` (line 384)
- `sendToChannel(String, NotificationBuilder)` (line 551)
- `sendWithRetry(NotificationChannel, Notification, RetryPolicy)` (line 627)
- `getExecutor()` (line 529)

Replace inline listener calls with `eventBus.publish(...)`.

```java
// notify-core/src/main/java/io/notifyhub/core/NotificationExecutor.java
package io.notifyhub.core;

import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
import io.notifyhub.core.channel.SendResult;
import io.notifyhub.core.dedup.DeduplicationStore;
import io.notifyhub.core.dlq.DeadLetter;
import io.notifyhub.core.dlq.DeadLetterQueue;
import io.notifyhub.core.event.EventType;
import io.notifyhub.core.event.NotificationEvent;
import io.notifyhub.core.event.NotificationEventBus;
import io.notifyhub.core.ratelimit.RateLimitExceededException;
import io.notifyhub.core.ratelimit.RateLimiter;
import io.notifyhub.core.retry.RetryPolicy;
import io.notifyhub.core.template.TemplateEngine;
import io.notifyhub.core.template.VersionedTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

class NotificationExecutor {

    private static final Logger log = LoggerFactory.getLogger(NotificationExecutor.class);

    private final Map<String, NotificationChannel> channels;
    private final TemplateEngine templateEngine;
    private final RetryPolicy defaultRetryPolicy;
    private final NotificationEventBus eventBus;
    private final DeduplicationStore deduplicationStore;
    private final RateLimiter rateLimiter;
    private final DeadLetterQueue deadLetterQueue;
    private final NotificationTracker tracker;
    private final ExecutorService asyncExecutor;

    NotificationExecutor(Map<String, NotificationChannel> channels,
                         TemplateEngine templateEngine,
                         RetryPolicy defaultRetryPolicy,
                         NotificationEventBus eventBus,
                         DeduplicationStore deduplicationStore,
                         RateLimiter rateLimiter,
                         DeadLetterQueue deadLetterQueue,
                         NotificationTracker tracker) {
        this(channels, templateEngine, defaultRetryPolicy, eventBus,
             deduplicationStore, rateLimiter, deadLetterQueue, tracker, null);
    }

    NotificationExecutor(Map<String, NotificationChannel> channels,
                         TemplateEngine templateEngine,
                         RetryPolicy defaultRetryPolicy,
                         NotificationEventBus eventBus,
                         DeduplicationStore deduplicationStore,
                         RateLimiter rateLimiter,
                         DeadLetterQueue deadLetterQueue,
                         NotificationTracker tracker,
                         ExecutorService asyncExecutor) {
        this.channels = channels;
        this.templateEngine = templateEngine;
        this.defaultRetryPolicy = defaultRetryPolicy != null ? defaultRetryPolicy : RetryPolicy.none();
        this.eventBus = eventBus;
        this.deduplicationStore = deduplicationStore;
        this.rateLimiter = rateLimiter;
        this.deadLetterQueue = deadLetterQueue;
        this.tracker = tracker;
        this.asyncExecutor = asyncExecutor;
    }

    // All execute/executeAll/executeAsync/executeTracked/sendToChannel/sendWithRetry
    // methods moved here verbatim from NotifyHub, replacing notifyListeners/notifyListenersSuccess
    // calls with eventBus.publish(...) calls.
    // See NotifyHub.java lines 248-697 for the source code to move.

    // Key change: instead of:
    //   notifyListenersSuccess(channelName, builder);
    // use:
    //   eventBus.publish(NotificationEvent.builder()
    //       .type(EventType.SENT).channelName(channelName)
    //       .recipient(recipient).templateName(builder.getTemplateName()).build());

    // And instead of:
    //   notifyListeners(channelName, builder, e);
    // use:
    //   eventBus.publish(NotificationEvent.builder()
    //       .type(EventType.FAILED).channelName(channelName)
    //       .templateName(builder.getTemplateName()).errorMessage(e.getMessage()).build());
}
```

**Note:** The full implementation copies lines 248-697 from `NotifyHub.java` into this class, replacing all `notifyListeners*` calls with `eventBus.publish(...)` and adding `tracker` usage where needed. The constructor receives all dependencies that were fields in `NotifyHub`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn test -pl notify-core -Dtest="NotificationExecutorTest" -B`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add notify-core/src/main/java/io/notifyhub/core/NotificationExecutor.java
git add notify-core/src/test/java/io/notifyhub/core/NotificationExecutorTest.java
git commit -m "feat(core): extract NotificationExecutor from NotifyHub"
```

---

### Task 4: Extract NotificationScheduler from NotifyHub

**Files:**
- Create: `notify-core/src/main/java/io/notifyhub/core/NotificationScheduler.java`
- Create: `notify-core/src/test/java/io/notifyhub/core/NotificationSchedulerTest.java`

- [ ] **Step 1: Write failing tests**

```java
// notify-core/src/test/java/io/notifyhub/core/NotificationSchedulerTest.java
package io.notifyhub.core;

import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.event.EventType;
import io.notifyhub.core.event.NotificationEvent;
import io.notifyhub.core.event.NotificationEventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationSchedulerTest {

    @Mock private NotificationExecutor executor;
    @Mock private NotificationChannel emailChannel;
    private List<NotificationEvent> events;
    private NotificationEventBus eventBus;
    private ScheduledExecutorService scheduledExecutor;

    @BeforeEach
    void setUp() {
        when(emailChannel.getName()).thenReturn("email");
        when(emailChannel.sendWithResult(any())).thenCallRealMethod();
        events = new ArrayList<>();
        eventBus = new NotificationEventBus(List.of(events::add));
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "test-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    @Test
    @DisplayName("Should schedule notification and publish SCHEDULED event")
    void schedulePublishesEvent() {
        NotificationScheduler scheduler = new NotificationScheduler(executor, eventBus, scheduledExecutor, null);
        NotifyHub hub = NotifyHub.builder().channel(emailChannel).build();
        NotificationBuilder builder = new NotificationBuilder(hub)
            .to("user@test.com").via(Channel.EMAIL).content("Hello");

        ScheduledNotification result = scheduler.schedule(builder, Duration.ofHours(1));

        assertNotNull(result);
        assertFalse(result.isCancelled());
        assertTrue(events.stream().anyMatch(e -> e.type() == EventType.SCHEDULED));
    }

    @Test
    @DisplayName("Should publish CANCELLED event when cancelled")
    void cancelPublishesEvent() {
        NotificationScheduler scheduler = new NotificationScheduler(executor, eventBus, scheduledExecutor, null);
        NotifyHub hub = NotifyHub.builder().channel(emailChannel).build();
        NotificationBuilder builder = new NotificationBuilder(hub)
            .to("user@test.com").via(Channel.EMAIL).content("Hello");

        ScheduledNotification result = scheduler.schedule(builder, Duration.ofHours(1));
        result.cancel();

        assertTrue(result.isCancelled());
        assertTrue(events.stream().anyMatch(e -> e.type() == EventType.CANCELLED));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn test -pl notify-core -Dtest="NotificationSchedulerTest" -B`
Expected: FAIL

- [ ] **Step 3: Write NotificationScheduler — extract from NotifyHub lines 434-549**

```java
// notify-core/src/main/java/io/notifyhub/core/NotificationScheduler.java
package io.notifyhub.core;

import io.notifyhub.core.event.EventType;
import io.notifyhub.core.event.NotificationEvent;
import io.notifyhub.core.event.NotificationEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;

class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    private final NotificationExecutor executor;
    private final NotificationEventBus eventBus;
    private final ScheduledExecutorService scheduledExecutor;
    private final NotificationTracker tracker;
    private volatile ScheduledExecutorService defaultScheduler;

    NotificationScheduler(NotificationExecutor executor,
                          NotificationEventBus eventBus,
                          ScheduledExecutorService scheduledExecutor,
                          NotificationTracker tracker) {
        this.executor = executor;
        this.eventBus = eventBus;
        this.scheduledExecutor = scheduledExecutor;
        this.tracker = tracker;
    }

    // Move executeScheduled() from NotifyHub here.
    // Replace direct listener iteration with eventBus.publish(...)
    // Key: schedule() calls executor.execute(builder) when the time arrives.
    // Dependency flow: Scheduler → Executor (no circular)

    ScheduledNotification schedule(NotificationBuilder builder, Duration delay) {
        // ... moved from NotifyHub.executeScheduled() lines 440-525
        // Replace listener calls with eventBus.publish(...)
    }

    private ScheduledExecutorService getScheduler() {
        // ... moved from NotifyHub.getScheduler() lines 533-549
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn test -pl notify-core -Dtest="NotificationSchedulerTest" -B`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add notify-core/src/main/java/io/notifyhub/core/NotificationScheduler.java
git add notify-core/src/test/java/io/notifyhub/core/NotificationSchedulerTest.java
git commit -m "feat(core): extract NotificationScheduler from NotifyHub"
```

---

### Task 5: Refactor NotifyHub to delegate to extracted classes

**Files:**
- Modify: `notify-core/src/main/java/io/notifyhub/core/NotifyHub.java`
- Modify: `notify-core/src/test/java/io/notifyhub/core/NotifyHubTest.java`

- [ ] **Step 1: Refactor NotifyHub to use NotificationExecutor, NotificationScheduler, NotificationEventBus**

Replace the 14 fields with 5. The `Builder` constructs EventBus, Executor, and Scheduler internally.

Key changes:
- `NotifyHub` fields become: `channels`, `executor`, `scheduler`, `eventBus`, `audienceManager`
- `Builder.build()` creates: `NotificationEventBus` (wrapping legacy listeners), `NotificationExecutor`, `NotificationScheduler`
- `NotifyHub.execute(builder)` becomes `executor.execute(builder)`
- `NotifyHub.executeScheduled(builder, delay)` becomes `scheduler.schedule(builder, delay)`
- Public getter methods (`getTracker()`, `getDeadLetterQueue()`, etc.) delegate to executor internals
- Builder methods remain identical — same public API

- [ ] **Step 2: Run ALL existing tests**

Run: `mvn test -pl notify-core -B`
Expected: PASS — all existing `NotifyHubTest` tests pass unchanged because the public API hasn't changed

- [ ] **Step 3: Run full project tests**

Run: `mvn test -B`
Expected: PASS — no module breaks

- [ ] **Step 4: Commit**

```bash
git add notify-core/src/main/java/io/notifyhub/core/NotifyHub.java
git add notify-core/src/test/java/io/notifyhub/core/NotifyHubTest.java
git commit -m "refactor(core): NotifyHub delegates to NotificationExecutor, NotificationScheduler, NotificationEventBus"
```

---

### Task 6: Split NotifyProperties inner classes

**Files:**
- Modify: `notify-spring-boot-starter/src/main/java/io/notifyhub/spring/NotifyProperties.java`
- Create: `notify-spring-boot-starter/src/main/java/io/notifyhub/spring/properties/EmailProperties.java`
- Create: `notify-spring-boot-starter/src/main/java/io/notifyhub/spring/properties/SmsProperties.java`
- Create: one file per channel (22 total)

- [ ] **Step 1: Create properties package and extract each inner class**

For each inner class in `NotifyProperties` (Email, Sms, Slack, etc.), move to a top-level class in `io.notifyhub.spring.properties`. Keep the `Channels` class in `NotifyProperties` but change field types to reference the extracted classes.

Example for Email:
```java
// notify-spring-boot-starter/src/main/java/io/notifyhub/spring/properties/EmailProperties.java
package io.notifyhub.spring.properties;

public class EmailProperties {
    private String host;
    private int port = 587;
    private String username;
    private String password;
    private String from;
    private boolean starttls = true;
    // ... getters/setters from NotifyProperties.Email
}
```

In `NotifyProperties`, change:
```java
// Before:
public static class Channels {
    private Email email;
    // ...
}

// After:
public static class Channels {
    private EmailProperties email;
    // ...
}
```

- [ ] **Step 2: Run full starter tests**

Run: `mvn test -pl notify-spring-boot-starter -B`
Expected: PASS

- [ ] **Step 3: Run full project tests**

Run: `mvn test -B`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add notify-spring-boot-starter/src/main/java/io/notifyhub/spring/
git commit -m "refactor(starter): extract NotifyProperties inner classes to separate files"
```

---

### Task 7: Split DemoController

**Files:**
- Modify: `notify-demo/src/main/java/io/notifyhub/demo/DemoController.java`
- Create: `notify-demo/src/main/java/io/notifyhub/demo/DemoEmailController.java`
- Create: `notify-demo/src/main/java/io/notifyhub/demo/DemoSmsController.java`
- Create: `notify-demo/src/main/java/io/notifyhub/demo/DemoBatchController.java`
- Create: `notify-demo/src/main/java/io/notifyhub/demo/DemoSchedulingController.java`
- Create: `notify-demo/src/main/java/io/notifyhub/demo/DemoSocialController.java`
- Create: `notify-demo/src/main/java/io/notifyhub/demo/DemoChatController.java`

- [ ] **Step 1: Split endpoints by domain**

Group demo endpoints:
- `DemoEmailController` — `/send/email`, `/send/template`, `/send/notifiable`
- `DemoSmsController` — `/send/sms`, `/send/whatsapp`
- `DemoChatController` — `/send/slack`, `/send/discord`, `/send/teams`, `/send/telegram`, `/send/google-chat`
- `DemoSocialController` — `/send/twitter`, `/send/linkedin`, `/send/facebook`, `/send/instagram`, `/send/youtube`, `/send/twitch`, `/send/tiktok-shop`
- `DemoBatchController` — `/send/batch`, `/send/audience`, `/send/multi-channel`
- `DemoSchedulingController` — `/send/scheduled`, `/cancel-scheduled`
- `DemoController` — remains with `/api/info`, `/` (home page), and shared endpoints

Each controller receives `NotifyHub` via constructor injection.

- [ ] **Step 2: Run demo app to verify**

Run: `mvn spring-boot:run -pl notify-demo -DskipTests`
Verify: App starts, endpoints respond

- [ ] **Step 3: Commit**

```bash
git add notify-demo/src/main/java/io/notifyhub/demo/
git commit -m "refactor(demo): split DemoController into domain-specific controllers"
```

---

## Chunk 2: Resilience Pipeline

### Task 8: Create SendHandler interface and SendContext

**Files:**
- Create: `notify-core/src/main/java/io/notifyhub/core/pipeline/SendHandler.java`
- Create: `notify-core/src/main/java/io/notifyhub/core/pipeline/SendContext.java`
- Create: `notify-core/src/test/java/io/notifyhub/core/pipeline/SendContextTest.java`

- [ ] **Step 1: Write SendHandler and SendContext**

```java
// notify-core/src/main/java/io/notifyhub/core/pipeline/SendHandler.java
package io.notifyhub.core.pipeline;

import io.notifyhub.core.channel.SendResult;

public interface SendHandler {
    SendResult handle(SendContext context, SendHandler next);
}
```

```java
// notify-core/src/main/java/io/notifyhub/core/pipeline/SendContext.java
package io.notifyhub.core.pipeline;

import io.notifyhub.core.Notification;
import io.notifyhub.core.NotificationBuilder;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.retry.RetryPolicy;

public class SendContext {
    private final String channelName;
    private final NotificationChannel channel;
    private final NotificationBuilder builder;
    private final RetryPolicy retryPolicy;
    private Notification notification;  // set by template handler

    // Constructor, getters, setters
}
```

- [ ] **Step 2: Commit**

```bash
git add notify-core/src/main/java/io/notifyhub/core/pipeline/
git commit -m "feat(core): add SendHandler interface and SendContext for pipeline"
```

---

### Task 9: Create pipeline handlers (Dedup, RateLimit, Template, Retry, DLQ)

**Files:**
- Create: `notify-core/src/main/java/io/notifyhub/core/pipeline/DeduplicationHandler.java`
- Create: `notify-core/src/main/java/io/notifyhub/core/pipeline/RateLimitHandler.java`
- Create: `notify-core/src/main/java/io/notifyhub/core/pipeline/TemplateHandler.java`
- Create: `notify-core/src/main/java/io/notifyhub/core/pipeline/RetrySendHandler.java`
- Create: `notify-core/src/main/java/io/notifyhub/core/pipeline/ChannelSendHandler.java`
- Create: `notify-core/src/test/java/io/notifyhub/core/pipeline/PipelineTest.java`

- [ ] **Step 1: Write failing pipeline integration test**

Test that the full pipeline Dedup → RateLimit → Template → Retry(Send) works end-to-end.

- [ ] **Step 2: Implement each handler**

Each handler calls `next.handle(context, ...)` to pass to the next step.
`RetrySendHandler` wraps `ChannelSendHandler` — on failure, it re-invokes the inner handler.

- [ ] **Step 3: Run tests to verify they pass**

Run: `mvn test -pl notify-core -Dtest="PipelineTest" -B`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add notify-core/src/main/java/io/notifyhub/core/pipeline/
git add notify-core/src/test/java/io/notifyhub/core/pipeline/
git commit -m "feat(core): implement pipeline handlers (dedup, ratelimit, template, retry, send)"
```

---

### Task 10: Create ChannelCircuitBreaker

**Files:**
- Create: `notify-core/src/main/java/io/notifyhub/core/resilience/ChannelCircuitBreaker.java`
- Create: `notify-core/src/main/java/io/notifyhub/core/resilience/CircuitBreakerConfig.java`
- Create: `notify-core/src/main/java/io/notifyhub/core/resilience/CircuitState.java`
- Create: `notify-core/src/main/java/io/notifyhub/core/pipeline/CircuitBreakerHandler.java`
- Create: `notify-core/src/test/java/io/notifyhub/core/resilience/ChannelCircuitBreakerTest.java`

- [ ] **Step 1: Write failing tests**

Test state transitions: CLOSED → OPEN after threshold, OPEN → HALF_OPEN after timeout, HALF_OPEN → CLOSED on success.

- [ ] **Step 2: Implement CircuitBreaker**

```java
public class ChannelCircuitBreaker {
    private final int failureThreshold;
    private final Duration openDuration;
    private final Duration windowSize;
    // Per-channel state tracking via ConcurrentHashMap<String, CircuitState>

    public boolean allowRequest(String channelName) { ... }
    public void recordSuccess(String channelName) { ... }
    public void recordFailure(String channelName) { ... }
}
```

- [ ] **Step 3: Run tests, verify pass**

Run: `mvn test -pl notify-core -Dtest="ChannelCircuitBreakerTest" -B`

- [ ] **Step 4: Commit**

```bash
git add notify-core/src/main/java/io/notifyhub/core/resilience/
git add notify-core/src/main/java/io/notifyhub/core/pipeline/CircuitBreakerHandler.java
git add notify-core/src/test/java/io/notifyhub/core/resilience/
git commit -m "feat(core): add circuit breaker per channel with pipeline handler"
```

---

### Task 11: Create BulkheadConfig and wire to builder

**Files:**
- Create: `notify-core/src/main/java/io/notifyhub/core/resilience/BulkheadConfig.java`
- Modify: `notify-core/src/main/java/io/notifyhub/core/NotifyHub.java` (add builder methods)
- Create: `notify-core/src/test/java/io/notifyhub/core/resilience/BulkheadConfigTest.java`

- [ ] **Step 1: Write BulkheadConfig with tests**
- [ ] **Step 2: Add `.circuitBreaker()` and `.bulkhead()` to NotifyHub.Builder**
- [ ] **Step 3: Run all tests**

Run: `mvn test -pl notify-core -B`

- [ ] **Step 4: Commit**

```bash
git add notify-core/src/main/java/io/notifyhub/core/resilience/BulkheadConfig.java
git add notify-core/src/main/java/io/notifyhub/core/NotifyHub.java
git add notify-core/src/test/java/io/notifyhub/core/resilience/
git commit -m "feat(core): add BulkheadConfig and wire circuit breaker + bulkhead to builder"
```

---

### Task 12: Enhance health check in Spring starter

**Files:**
- Modify: `notify-spring-boot-starter/src/main/java/io/notifyhub/spring/NotifyAutoConfiguration.java`

- [ ] **Step 1: Enhance NotifyHubHealthIndicator to report per-channel and circuit breaker state**
- [ ] **Step 2: Run starter tests**

Run: `mvn test -pl notify-spring-boot-starter -B`

- [ ] **Step 3: Commit**

```bash
git add notify-spring-boot-starter/
git commit -m "feat(starter): enhance health indicator with per-channel + circuit breaker status"
```

---

## Chunk 3: Unified Observability

### Task 13: Create MetricsEventListener

**Files:**
- Create: `notify-spring-boot-starter/src/main/java/io/notifyhub/spring/metrics/MetricsEventListener.java`
- Create: `notify-spring-boot-starter/src/test/java/io/notifyhub/spring/metrics/MetricsEventListenerTest.java`

- [ ] **Step 1: Write failing tests**

Test that SENT events produce counter + timer, FAILED events produce counter, RATE_LIMITED produces counter.

- [ ] **Step 2: Implement MetricsEventListener using Micrometer MeterRegistry**

```java
@ConditionalOnClass(MeterRegistry.class)
public class MetricsEventListener implements NotificationEventListener {
    private final MeterRegistry registry;

    public void onEvent(NotificationEvent event) {
        switch (event.type()) {
            case SENT -> {
                registry.counter("notifyhub.send.total",
                    "channel", event.channelName(), "status", "success").increment();
                if (event.latency() != null) {
                    registry.timer("notifyhub.send.latency",
                        "channel", event.channelName()).record(event.latency());
                }
            }
            case FAILED -> registry.counter("notifyhub.send.total",
                "channel", event.channelName(), "status", "failed").increment();
            case RATE_LIMITED -> registry.counter("notifyhub.rate_limit.rejected",
                "channel", event.channelName()).increment();
            case RETRIED -> registry.counter("notifyhub.retry.total",
                "channel", event.channelName()).increment();
            default -> {}
        }
    }
}
```

- [ ] **Step 3: Run tests, wire in auto-config**
- [ ] **Step 4: Commit**

```bash
git add notify-spring-boot-starter/src/main/java/io/notifyhub/spring/metrics/
git add notify-spring-boot-starter/src/test/java/io/notifyhub/spring/metrics/
git commit -m "feat(starter): add MetricsEventListener for automatic Micrometer metrics"
```

---

### Task 14: Wire EventBus into Spring auto-configuration

**Files:**
- Modify: `notify-spring-boot-starter/src/main/java/io/notifyhub/spring/NotifyAutoConfiguration.java`

- [ ] **Step 1: Register NotificationEventBus bean, auto-register MetricsEventListener + LegacyListenerAdapter**
- [ ] **Step 2: Run full tests**

Run: `mvn test -B`

- [ ] **Step 3: Commit**

```bash
git add notify-spring-boot-starter/
git commit -m "feat(starter): wire NotificationEventBus into Spring auto-configuration"
```

---

## Chunk 4: DX Improvements

### Task 15: Organize attachment package

**Files:**
- Move: `notify-core/src/main/java/io/notifyhub/core/Attachment.java` → `notify-core/src/main/java/io/notifyhub/core/attachment/Attachment.java`
- Update: all imports referencing `io.notifyhub.core.Attachment`

- [ ] **Step 1: Move Attachment class and update imports across all modules**

Use IDE-style refactoring: move file, update package declaration, update all imports.

Run: `mvn test -B` after each change to verify nothing breaks.

- [ ] **Step 2: Commit**

```bash
git add -A
git commit -m "refactor(core): move Attachment to io.notifyhub.core.attachment package"
```

---

### Task 16: Better error messages (Levenshtein suggestion)

**Files:**
- Modify: `notify-core/src/main/java/io/notifyhub/core/NotificationExecutor.java`
- Create: `notify-core/src/test/java/io/notifyhub/core/ChannelSuggestionTest.java`

- [ ] **Step 1: Write failing test**

```java
@Test
@DisplayName("Should suggest closest channel name on typo")
void suggestClosestChannel() {
    // Register "slack", try to send via "slck"
    // Expect error message containing "Did you mean 'slack'?"
}
```

- [ ] **Step 2: Implement Levenshtein distance (simple, ~20 lines)**

In `sendToChannel`, when channel not found, compute distance to all registered channels and suggest closest if distance <= 2.

- [ ] **Step 3: Run tests, verify pass**
- [ ] **Step 4: Commit**

```bash
git add notify-core/
git commit -m "feat(core): suggest closest channel name on registration typo"
```

---

### Task 17: Create TestNotifyHub for library consumers

**Files:**
- Create: `notify-core/src/main/java/io/notifyhub/core/testing/TestNotifyHub.java`
- Create: `notify-core/src/main/java/io/notifyhub/core/testing/SentNotification.java`
- Create: `notify-core/src/test/java/io/notifyhub/core/testing/TestNotifyHubTest.java`

- [ ] **Step 1: Write failing tests**

```java
@Test
@DisplayName("Should capture sent notifications for assertions")
void captureSentNotifications() {
    TestNotifyHub testHub = TestNotifyHub.create();
    testHub.to("user@test.com").via(Channel.EMAIL).content("Hello").send();

    assertEquals(1, testHub.sent().size());
    assertEquals("email", testHub.sent().get(0).channel());
    assertEquals("user@test.com", testHub.sent().get(0).recipient());
    assertEquals("Hello", testHub.sent().get(0).content());
}
```

- [ ] **Step 2: Implement TestNotifyHub**

Creates a `NotifyHub` with mock channels that capture all sends. `TestNotifyHub.create()` registers mock channels for all `Channel` enum values.

- [ ] **Step 3: Run tests, verify pass**
- [ ] **Step 4: Commit**

```bash
git add notify-core/src/main/java/io/notifyhub/core/testing/
git add notify-core/src/test/java/io/notifyhub/core/testing/
git commit -m "feat(core): add TestNotifyHub for easy testing by library consumers"
```

---

### Task 18: Create channel template/archetype module

**Files:**
- Create: `notify-channels/notify-channel-template/pom.xml`
- Create: `notify-channels/notify-channel-template/src/main/java/io/notifyhub/channel/template/TemplateChannelConfig.java`
- Create: `notify-channels/notify-channel-template/src/main/java/io/notifyhub/channel/template/TemplateChannel.java`
- Create: `notify-channels/notify-channel-template/src/test/java/io/notifyhub/channel/template/TemplateChannelTest.java`
- Create: `notify-channels/notify-channel-template/README.md`

- [ ] **Step 1: Create archetype with TODO placeholders**
- [ ] **Step 2: Verify it compiles**

Run: `mvn compile -pl notify-channels/notify-channel-template -B`

- [ ] **Step 3: Commit**

```bash
git add notify-channels/notify-channel-template/
git commit -m "feat(channels): add channel template/archetype for easy new channel creation"
```

---

## Chunk 5: New Features

### Task 19: Add opt-out and quiet hours to Notifiable

**Files:**
- Modify: `notify-core/src/main/java/io/notifyhub/core/Notifiable.java`
- Create: `notify-core/src/main/java/io/notifyhub/core/QuietHours.java`
- Create: `notify-core/src/test/java/io/notifyhub/core/QuietHoursTest.java`

- [ ] **Step 1: Write failing QuietHours tests**

```java
@Test
@DisplayName("Should return now when not in quiet period")
void notInQuietPeriod() {
    QuietHours qh = QuietHours.between(LocalTime.of(22, 0), LocalTime.of(8, 0),
        ZoneId.of("America/Sao_Paulo"));
    Instant noon = ZonedDateTime.of(2026, 3, 17, 12, 0, 0, 0,
        ZoneId.of("America/Sao_Paulo")).toInstant();
    assertEquals(noon, qh.nextAllowedTime(noon));
}

@Test
@DisplayName("Should return next morning when in quiet period")
void inQuietPeriod() {
    QuietHours qh = QuietHours.between(LocalTime.of(22, 0), LocalTime.of(8, 0),
        ZoneId.of("America/Sao_Paulo"));
    Instant lateNight = ZonedDateTime.of(2026, 3, 17, 23, 30, 0, 0,
        ZoneId.of("America/Sao_Paulo")).toInstant();
    Instant nextMorning = ZonedDateTime.of(2026, 3, 18, 8, 0, 0, 0,
        ZoneId.of("America/Sao_Paulo")).toInstant();
    assertEquals(nextMorning, qh.nextAllowedTime(lateNight));
}

@Test
@DisplayName("QuietHours.none() always returns now")
void noneAlwaysReturnsNow() {
    Instant now = Instant.now();
    assertEquals(now, QuietHours.none().nextAllowedTime(now));
}
```

- [ ] **Step 2: Implement QuietHours**
- [ ] **Step 3: Add default methods to Notifiable**

```java
default Set<Channel> getOptedOutChannels() { return Set.of(); }
default QuietHours getQuietHours() { return QuietHours.none(); }
```

- [ ] **Step 4: Wire opt-out and quiet hours into NotificationExecutor pipeline**
- [ ] **Step 5: Run all tests**

Run: `mvn test -B`

- [ ] **Step 6: Commit**

```bash
git add notify-core/
git commit -m "feat(core): add opt-out channels and quiet hours to Notifiable"
```

---

### Task 20: Create OrchestrationBuilder for multi-channel orchestration

**Files:**
- Create: `notify-core/src/main/java/io/notifyhub/core/orchestration/OrchestrationBuilder.java`
- Create: `notify-core/src/main/java/io/notifyhub/core/orchestration/OrchestrationStep.java`
- Create: `notify-core/src/main/java/io/notifyhub/core/orchestration/OrchestrationExecutor.java`
- Create: `notify-core/src/test/java/io/notifyhub/core/orchestration/OrchestrationBuilderTest.java`
- Modify: `notify-core/src/main/java/io/notifyhub/core/NotifyHub.java` (add `orchestrate()`)

- [ ] **Step 1: Write failing tests for orchestration builder API**

```java
@Test
@DisplayName("Should build orchestration with email then push fallback")
void buildOrchestration() {
    OrchestrationBuilder builder = hub.to(user).orchestrate();
    builder.first(Channel.EMAIL).template("promo")
           .ifNoOpen(Duration.ofHours(24))
           .then(Channel.PUSH).content("Check your email!");

    List<OrchestrationStep> steps = builder.getSteps();
    assertEquals(2, steps.size());
    assertEquals("email", steps.get(0).channelName());
    assertEquals(Duration.ofHours(24), steps.get(0).escalateAfter());
}
```

- [ ] **Step 2: Implement OrchestrationBuilder + OrchestrationStep**
- [ ] **Step 3: Implement OrchestrationExecutor (uses NotificationScheduler + NotificationTracker)**
- [ ] **Step 4: Add `.orchestrate()` to NotifyHub**
- [ ] **Step 5: Run tests, verify pass**
- [ ] **Step 6: Commit**

```bash
git add notify-core/src/main/java/io/notifyhub/core/orchestration/
git add notify-core/src/test/java/io/notifyhub/core/orchestration/
git add notify-core/src/main/java/io/notifyhub/core/NotifyHub.java
git commit -m "feat(core): add multi-channel orchestration builder"
```

---

### Task 21: Create A/B Testing support

**Files:**
- Create: `notify-core/src/main/java/io/notifyhub/core/abtest/AbTestBuilder.java`
- Create: `notify-core/src/main/java/io/notifyhub/core/abtest/AbTestVariant.java`
- Create: `notify-core/src/test/java/io/notifyhub/core/abtest/AbTestBuilderTest.java`
- Modify: `notify-core/src/main/java/io/notifyhub/core/NotificationBuilder.java` (add `abTest()`)

- [ ] **Step 1: Write failing tests for deterministic variant assignment**
- [ ] **Step 2: Implement AbTestBuilder with hash-based variant selection**
- [ ] **Step 3: Wire into NotificationBuilder**
- [ ] **Step 4: Run tests, verify pass**
- [ ] **Step 5: Commit**

```bash
git add notify-core/src/main/java/io/notifyhub/core/abtest/
git add notify-core/src/test/java/io/notifyhub/core/abtest/
git add notify-core/src/main/java/io/notifyhub/core/NotificationBuilder.java
git commit -m "feat(core): add A/B testing support with deterministic variant assignment"
```

---

### Task 22: Create Cron Scheduling

**Files:**
- Create: `notify-core/src/main/java/io/notifyhub/core/schedule/CronExpression.java`
- Create: `notify-core/src/test/java/io/notifyhub/core/schedule/CronExpressionTest.java`
- Modify: `notify-core/src/main/java/io/notifyhub/core/NotificationBuilder.java` (add `cron()`)

- [ ] **Step 1: Write failing CronExpression tests**

```java
@Test
@DisplayName("Should compute next fire time for weekday mornings")
void weekdayMornings() {
    CronExpression cron = CronExpression.parse("0 9 * * MON-FRI");
    // Given Monday 10:00, next should be Tuesday 09:00
    Instant monday10 = ZonedDateTime.of(2026, 3, 16, 10, 0, 0, 0,
        ZoneId.of("UTC")).toInstant();
    Instant tuesday9 = ZonedDateTime.of(2026, 3, 17, 9, 0, 0, 0,
        ZoneId.of("UTC")).toInstant();
    assertEquals(tuesday9, cron.nextFireTime(monday10, ZoneId.of("UTC")));
}
```

- [ ] **Step 2: Implement lightweight CronExpression parser (~100 lines)**
- [ ] **Step 3: Wire `.cron()` into NotificationBuilder → NotificationScheduler**
- [ ] **Step 4: Run tests**
- [ ] **Step 5: Commit**

```bash
git add notify-core/src/main/java/io/notifyhub/core/schedule/
git add notify-core/src/test/java/io/notifyhub/core/schedule/
git add notify-core/src/main/java/io/notifyhub/core/NotificationBuilder.java
git commit -m "feat(core): add cron scheduling with lightweight internal parser"
```

---

### Task 23: Final integration test and full build

- [ ] **Step 1: Run full project build**

Run: `mvn clean verify -B`
Expected: BUILD SUCCESS

- [ ] **Step 2: Run GitNexus re-index to validate architecture**

Run: `gitnexus analyze .`
Expected: Fewer unnamed clusters, more cohesive module structure

- [ ] **Step 3: Final commit if any cleanup needed**

```bash
git commit -m "chore: final cleanup after architecture improvements"
```
