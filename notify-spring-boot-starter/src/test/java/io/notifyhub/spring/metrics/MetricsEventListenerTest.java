package io.notifyhub.spring.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.notifyhub.core.event.EventType;
import io.notifyhub.core.event.NotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class MetricsEventListenerTest {

    private SimpleMeterRegistry registry;
    private MetricsEventListener listener;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        listener = new MetricsEventListener(registry);
    }

    @Test
    @DisplayName("Should increment send counter on SENT event")
    void sentEvent() {
        listener.onEvent(NotificationEvent.builder(EventType.SENT, "email").build());
        assertEquals(1.0, registry.counter("notifyhub.send.total", "channel", "email", "status", "success").count());
    }

    @Test
    @DisplayName("Should increment failed counter on FAILED event")
    void failedEvent() {
        listener.onEvent(NotificationEvent.builder(EventType.FAILED, "slack")
            .errorMessage("timeout").build());
        assertEquals(1.0, registry.counter("notifyhub.send.total", "channel", "slack", "status", "failed").count());
    }

    @Test
    @DisplayName("Should increment rate limit counter on RATE_LIMITED event")
    void rateLimitedEvent() {
        listener.onEvent(NotificationEvent.builder(EventType.RATE_LIMITED, "sms").build());
        assertEquals(1.0, registry.counter("notifyhub.rate_limit.rejected", "channel", "sms").count());
    }

    @Test
    @DisplayName("Should record latency timer on SENT event with latency")
    void sentWithLatency() {
        listener.onEvent(NotificationEvent.builder(EventType.SENT, "email")
            .latency(Duration.ofMillis(150)).build());
        assertNotNull(registry.find("notifyhub.send.latency").tag("channel", "email").timer());
    }

    @Test
    @DisplayName("Should increment retry counter on RETRIED event")
    void retriedEvent() {
        listener.onEvent(NotificationEvent.builder(EventType.RETRIED, "email").build());
        assertEquals(1.0, registry.counter("notifyhub.retry.total", "channel", "email").count());
    }
}
