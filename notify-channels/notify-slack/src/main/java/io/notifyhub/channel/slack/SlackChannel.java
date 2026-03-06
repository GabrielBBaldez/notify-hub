package io.notifyhub.channel.slack;

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
import java.util.Map;

/**
 * Slack notification channel using Incoming Webhooks.
 *
 * <p>Sends messages to a Slack channel via a webhook URL.
 * No external SDK needed — uses the JDK {@link HttpClient}.</p>
 *
 * <pre>{@code
 * SlackChannel slack = new SlackChannel(
 *     SlackConfig.builder()
 *         .webhookUrl("https://hooks.slack.com/services/XXX/YYY/ZZZ")
 *         .build()
 * );
 * }</pre>
 */
public class SlackChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(SlackChannel.class);

    private final SlackConfig config;
    private final HttpClient httpClient;

    public SlackChannel(SlackConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();
    }

    @Override
    public String getName() {
        return "slack";
    }

    @Override
    public void send(Notification notification) {
        String content = notification.getRenderedContent();
        String recipient = notification.getRecipient();

        String imageUrl = notification.getImageUrl();

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"text\": \"").append(escapeJson(content)).append("\"");
            sb.append(", \"channel\": \"").append(escapeJson(recipient)).append("\"");

            if (imageUrl != null && !imageUrl.isBlank()) {
                sb.append(", \"attachments\": [{\"image_url\": \"")
                  .append(escapeJson(imageUrl))
                  .append("\", \"text\": \"\"}]");
            }

            sb.append("}");
            String payload = sb.toString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(resolveWebhookUrl(notification.getRecipient())))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new NotificationSendException("slack",
                        "Slack webhook returned " + response.statusCode() + ": " + response.body());
            }

            log.debug("Slack message sent to '{}': {}", recipient, response.body());

        } catch (NotificationSendException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationSendException("slack",
                    "Failed to send Slack webhook: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        return (config.getWebhookUrl() != null && !config.getWebhookUrl().isBlank())
                || !config.getRecipients().isEmpty();
    }

    @Override
    public Map<String, String> getConfiguredRecipients() {
        return config.getRecipients();
    }

    private String resolveWebhookUrl(String recipient) {
        if (recipient != null && config.getRecipients().containsKey(recipient)) {
            return config.getRecipients().get(recipient);
        }
        if (recipient != null && recipient.startsWith("http")) {
            return recipient;
        }
        return config.getWebhookUrl();
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
