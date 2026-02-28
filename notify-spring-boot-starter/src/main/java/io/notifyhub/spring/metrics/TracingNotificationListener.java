package io.notifyhub.spring.metrics;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.notifyhub.core.NotificationListener;

import java.time.Duration;

/**
 * Notification listener that creates Micrometer {@link Observation} spans for every
 * notification event. When an OpenTelemetry bridge is on the classpath
 * ({@code micrometer-tracing-bridge-otel}), these observations are automatically
 * exported as distributed traces.
 *
 * <p>Observations created:</p>
 * <ul>
 *   <li>{@code notifyhub.send} — on success/failure, tagged with channel, template, outcome</li>
 *   <li>{@code notifyhub.schedule} — on schedule/cancel, tagged with channel, outcome</li>
 * </ul>
 *
 * <p>Enable by adding to your classpath:</p>
 * <pre>{@code
 * <dependency>
 *     <groupId>io.micrometer</groupId>
 *     <artifactId>micrometer-tracing-bridge-otel</artifactId>
 * </dependency>
 * <dependency>
 *     <groupId>io.opentelemetry</groupId>
 *     <artifactId>opentelemetry-exporter-otlp</artifactId>
 * </dependency>
 * }</pre>
 */
public class TracingNotificationListener implements NotificationListener {

    private final ObservationRegistry registry;

    public TracingNotificationListener(ObservationRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onSuccess(String channelName, String templateName) {
        Observation observation = Observation.start("notifyhub.send", registry);
        observation.lowCardinalityKeyValue("channel", channelName);
        observation.lowCardinalityKeyValue("template", templateName != null ? templateName : "none");
        observation.lowCardinalityKeyValue("outcome", "success");
        observation.stop();
    }

    @Override
    public void onFailure(String channelName, String templateName, Exception error) {
        Observation observation = Observation.start("notifyhub.send", registry);
        observation.lowCardinalityKeyValue("channel", channelName);
        observation.lowCardinalityKeyValue("template", templateName != null ? templateName : "none");
        observation.lowCardinalityKeyValue("outcome", "failure");
        observation.error(error);
        observation.stop();
    }

    @Override
    public void onScheduled(String channelName, String recipient, Duration delay) {
        Observation observation = Observation.start("notifyhub.schedule", registry);
        observation.lowCardinalityKeyValue("channel", channelName);
        observation.lowCardinalityKeyValue("outcome", "scheduled");
        observation.stop();
    }

    @Override
    public void onCancelled(String channelName, String recipient) {
        Observation observation = Observation.start("notifyhub.schedule", registry);
        observation.lowCardinalityKeyValue("channel", channelName);
        observation.lowCardinalityKeyValue("outcome", "cancelled");
        observation.stop();
    }
}
