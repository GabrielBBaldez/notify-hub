package io.notifyhub.channel.teams;

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
 * Microsoft Teams notification channel using Incoming Webhooks.
 *
 * <p>Sends messages to a Teams channel via a webhook URL.
 * No external SDK needed — uses the JDK {@link HttpClient}.</p>
 *
 * <pre>{@code
 * TeamsChannel teams = new TeamsChannel(
 *     TeamsConfig.builder()
 *         .webhookUrl("https://outlook.office.com/webhook/XXX/YYY/ZZZ")
 *         .build()
 * );
 * }</pre>
 */
public class TeamsChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(TeamsChannel.class);

    private final TeamsConfig config;
    private final HttpClient httpClient;

    public TeamsChannel(TeamsConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();
    }

    @Override
    public String getName() {
        return "teams";
    }

    @Override
    public void send(Notification notification) {
        String content = notification.getRenderedContent();
        String subject = notification.getSubject();

        try {
            String summary = subject != null && !subject.isBlank() ? escapeJson(subject) : escapeJson(content);
            String payload = String.format(
                    "{\"@type\":\"MessageCard\",\"summary\":\"%s\",\"text\":\"%s\"}",
                    summary,
                    escapeJson(content)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getWebhookUrl()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new NotificationSendException("teams",
                        "Teams webhook returned " + response.statusCode() + ": " + response.body());
            }

            log.debug("Teams message sent: {}", response.body());

        } catch (NotificationSendException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationSendException("teams",
                    "Failed to send Teams webhook: " + e.getMessage(), e);
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
