package io.notifyhub.channel.discord;

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
 * Discord notification channel using Webhooks.
 *
 * <p>Sends messages to a Discord channel via a webhook URL.
 * No external SDK needed — uses the JDK {@link HttpClient}.</p>
 *
 * <pre>{@code
 * DiscordChannel discord = new DiscordChannel(
 *     DiscordConfig.builder()
 *         .webhookUrl("https://discord.com/api/webhooks/123/abc")
 *         .username("NotifyHub Bot")
 *         .build()
 * );
 * }</pre>
 */
public class DiscordChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(DiscordChannel.class);

    private final DiscordConfig config;
    private final HttpClient httpClient;

    public DiscordChannel(DiscordConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();
    }

    @Override
    public String getName() {
        return "discord";
    }

    @Override
    public void send(Notification notification) {
        String content = notification.getRenderedContent();

        try {
            // Per-notification override via params, fallback to config
            String username = resolveParam(notification, "senderName", config.getUsername());
            String avatarUrl = resolveParam(notification, "senderAvatar", config.getAvatarUrl());

            StringBuilder payload = new StringBuilder();
            payload.append("{\"content\": \"").append(escapeJson(content)).append("\"");

            if (username != null && !username.isBlank()) {
                payload.append(", \"username\": \"").append(escapeJson(username)).append("\"");
            }
            if (avatarUrl != null && !avatarUrl.isBlank()) {
                payload.append(", \"avatar_url\": \"").append(escapeJson(avatarUrl)).append("\"");
            }

            payload.append("}");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(resolveWebhookUrl(notification.getRecipient())))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Discord returns 204 No Content on success
            if (response.statusCode() != 200 && response.statusCode() != 204) {
                throw new NotificationSendException("discord",
                        "Discord webhook returned " + response.statusCode() + ": " + response.body());
            }

            log.debug("Discord message sent: {}", response.statusCode());

        } catch (NotificationSendException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationSendException("discord",
                    "Failed to send Discord webhook: " + e.getMessage(), e);
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

    private String resolveParam(Notification notification, String paramName, String configDefault) {
        Object value = notification.getParams().get(paramName);
        if (value != null && !value.toString().isEmpty()) {
            return value.toString();
        }
        return configDefault;
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
