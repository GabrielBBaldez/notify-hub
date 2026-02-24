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

        try {
            String payload = String.format(
                    "{\"text\": \"%s\", \"channel\": \"%s\"}",
                    escapeJson(content),
                    escapeJson(recipient)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getWebhookUrl()))
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
        return config.getWebhookUrl() != null && !config.getWebhookUrl().isBlank();
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
