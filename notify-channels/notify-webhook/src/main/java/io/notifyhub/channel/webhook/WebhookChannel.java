package io.notifyhub.channel.webhook;

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
 * Generic webhook notification channel.
 *
 * <p>Sends notifications to any HTTP endpoint using a configurable
 * payload template. Placeholders {@code {{recipient}}}, {@code {{subject}}},
 * and {@code {{content}}} are replaced with actual notification values.</p>
 *
 * <pre>{@code
 * WebhookChannel webhook = new WebhookChannel(
 *     WebhookConfig.builder()
 *         .name("order-webhook")
 *         .url("https://api.example.com/notifications")
 *         .build()
 * );
 * }</pre>
 */
public class WebhookChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(WebhookChannel.class);

    private final WebhookConfig config;
    private final HttpClient httpClient;

    public WebhookChannel(WebhookConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();
    }

    @Override
    public String getName() {
        return config.getName();
    }

    @Override
    public void send(Notification notification) {
        String content = notification.getRenderedContent();
        String recipient = notification.getRecipient();
        String subject = notification.getSubject();
        String imageUrl = notification.getImageUrl();

        try {
            String payload = renderPayload(config.getPayloadTemplate(), recipient, subject, content, imageUrl);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(config.getUrl()))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(config.getTimeoutMs()));

            config.getHeaders().forEach(requestBuilder::header);

            HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.ofString(payload);
            switch (config.getMethod().toUpperCase()) {
                case "PUT":
                    requestBuilder.PUT(bodyPublisher);
                    break;
                case "POST":
                default:
                    requestBuilder.POST(bodyPublisher);
                    break;
            }

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new NotificationSendException(getName(),
                        "Webhook returned " + status + ": " + response.body());
            }

            log.debug("Webhook '{}' sent to '{}': {}", getName(), recipient, response.body());

        } catch (NotificationSendException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationSendException(getName(),
                    "Failed to send webhook: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        return config.getUrl() != null && !config.getUrl().isBlank();
    }

    /**
     * Replaces {@code {{recipient}}}, {@code {{subject}}}, and {@code {{content}}}
     * placeholders in the template with escaped values.
     */
    String renderPayload(String template, String recipient, String subject, String content, String imageUrl) {
        return template
                .replace("{{recipient}}", escapeJson(recipient))
                .replace("{{subject}}", escapeJson(subject))
                .replace("{{content}}", escapeJson(content))
                .replace("{{imageUrl}}", escapeJson(imageUrl));
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
