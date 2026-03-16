package io.notifyhub.channel.pagerduty;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * PagerDuty notification channel using the Events API v2.
 *
 * <p>Triggers PagerDuty incidents via the Events API v2 endpoint.
 * No external SDK needed — uses the JDK {@link HttpClient}.</p>
 *
 * <pre>{@code
 * PagerDutyChannel pagerDuty = new PagerDutyChannel(
 *     PagerDutyConfig.builder()
 *         .routingKey("your-integration-key")
 *         .severity("critical")
 *         .build()
 * );
 * }</pre>
 */
public class PagerDutyChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(PagerDutyChannel.class);
    private static final String EVENTS_API_URL = "https://events.pagerduty.com/v2/enqueue";
    private static final int MAX_SUMMARY_LENGTH = 1024;

    private final PagerDutyConfig config;
    private final HttpClient httpClient;

    public PagerDutyChannel(PagerDutyConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();
    }

    /** Package-private constructor for testing with a mock/stub HttpClient. */
    PagerDutyChannel(PagerDutyConfig config, HttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
    }

    @Override
    public String getName() {
        return "pagerduty";
    }

    @Override
    public boolean isAvailable() {
        return config.getRoutingKey() != null && !config.getRoutingKey().isBlank();
    }

    @Override
    public void send(Notification notification) throws NotificationSendException {
        String content = notification.getRenderedContent();
        String summary = truncate(content, MAX_SUMMARY_LENGTH);

        // Allow per-notification severity override via params
        String severity = resolveParam(notification, "severity", config.getSeverity());

        String payload = buildPayload(summary, severity);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(EVENTS_API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // PagerDuty Events API returns 202 Accepted on success
            if (response.statusCode() != 202) {
                throw new NotificationSendException("pagerduty",
                        "PagerDuty API returned " + response.statusCode() + ": " + response.body());
            }

            log.debug("PagerDuty incident triggered: {}", response.statusCode());

        } catch (NotificationSendException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationSendException("pagerduty",
                    "Failed to trigger PagerDuty incident: " + e.getMessage(), e);
        }
    }

    private String buildPayload(String summary, String severity) {
        return "{"
                + "\"routing_key\": \"" + escapeJson(config.getRoutingKey()) + "\","
                + "\"event_action\": \"trigger\","
                + "\"payload\": {"
                + "\"summary\": \"" + escapeJson(summary) + "\","
                + "\"severity\": \"" + escapeJson(severity) + "\","
                + "\"source\": \"notifyhub\""
                + "}"
                + "}";
    }

    private String resolveParam(Notification notification, String paramName, String configDefault) {
        Object value = notification.getParams().get(paramName);
        if (value != null && !value.toString().isEmpty()) {
            return value.toString();
        }
        return configDefault;
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
