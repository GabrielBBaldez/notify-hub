package io.notifyhub.spring.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.notifyhub.core.event.NotificationEvent;
import io.notifyhub.core.event.NotificationEventListener;

public class MetricsEventListener implements NotificationEventListener {
    private final MeterRegistry registry;

    public MetricsEventListener(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
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
            case CIRCUIT_OPENED -> registry.gauge("notifyhub.circuit_breaker.state",
                io.micrometer.core.instrument.Tags.of("channel", event.channelName(), "state", "open"), 1);
            case CIRCUIT_CLOSED -> registry.gauge("notifyhub.circuit_breaker.state",
                io.micrometer.core.instrument.Tags.of("channel", event.channelName(), "state", "closed"), 0);
            case DEDUPED -> registry.counter("notifyhub.dedup.skipped",
                "channel", event.channelName()).increment();
            default -> { /* SCHEDULED, CANCELLED, HALF_OPEN — no metrics needed */ }
        }
    }
}
