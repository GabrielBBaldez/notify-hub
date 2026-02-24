package io.notifyhub.spring.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.notifyhub.core.NotificationListener;

import java.time.Duration;

/**
 * Notification listener that publishes Micrometer metrics for notification events.
 *
 * <p>Tracks the following counters:</p>
 * <ul>
 *   <li>{@code notifyhub.notifications.sent} — tagged by channel and template</li>
 *   <li>{@code notifyhub.notifications.failed} — tagged by channel and template</li>
 *   <li>{@code notifyhub.notifications.scheduled} — tagged by channel</li>
 * </ul>
 */
public class MicrometerNotificationListener implements NotificationListener {

    private final MeterRegistry meterRegistry;

    public MicrometerNotificationListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void onSuccess(String channelName, String templateName) {
        Counter.builder("notifyhub.notifications.sent")
                .tag("channel", channelName)
                .tag("template", templateName != null ? templateName : "none")
                .register(meterRegistry).increment();
    }

    @Override
    public void onFailure(String channelName, String templateName, Exception error) {
        Counter.builder("notifyhub.notifications.failed")
                .tag("channel", channelName)
                .tag("template", templateName != null ? templateName : "none")
                .register(meterRegistry).increment();
    }

    @Override
    public void onScheduled(String channelName, String recipient, Duration delay) {
        Counter.builder("notifyhub.notifications.scheduled")
                .tag("channel", channelName)
                .register(meterRegistry).increment();
    }
}
