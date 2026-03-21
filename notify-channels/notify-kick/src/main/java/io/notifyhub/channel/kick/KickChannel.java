package io.notifyhub.channel.kick;

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

public class KickChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(KickChannel.class);
    private static final String CHAT_URL = "https://api.kick.com/public/v1/chat";
    private static final int MAX_MESSAGE_LENGTH = 500;

    private final KickConfig config;
    private final HttpClient httpClient;

    public KickChannel(KickConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();
    }

    @Override
    public String getName() {
        return "kick";
    }

    @Override
    public void send(Notification notification) {
        String content = notification.getRenderedContent();
        String broadcasterId = resolveBroadcasterId(notification.getRecipient());
        String messageType = resolveMessageType(notification);

        if (content.length() > MAX_MESSAGE_LENGTH) {
            log.warn("Kick message exceeds {} chars, truncating", MAX_MESSAGE_LENGTH);
            content = content.substring(0, MAX_MESSAGE_LENGTH - 3) + "...";
        }

        try {
            StringBuilder payload = new StringBuilder("{");
            payload.append("\"content\": \"").append(escapeJson(content)).append("\", ");
            payload.append("\"type\": \"").append(escapeJson(messageType)).append("\"");
            if ("user".equals(messageType)) {
                payload.append(", \"broadcaster_user_id\": ").append(broadcasterId);
            }
            payload.append("}");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CHAT_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getAccessToken())
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new NotificationSendException("kick",
                        "Kick API returned " + response.statusCode() + ": " + response.body());
            }

            String body = response.body();
            if (body.contains("\"is_sent\":false") || body.contains("\"is_sent\": false")) {
                throw new NotificationSendException("kick",
                        "Kick API reported message not sent: " + body);
            }

            log.debug("Kick chat message sent to broadcaster {}: {}", broadcasterId, response.statusCode());

        } catch (NotificationSendException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationSendException("kick",
                    "Failed to send Kick chat message: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        return config.getAccessToken() != null && !config.getAccessToken().isBlank();
    }

    @Override
    public Map<String, String> getConfiguredRecipients() {
        return config.getRecipients();
    }

    private String resolveBroadcasterId(String recipient) {
        if (recipient != null && config.getRecipients().containsKey(recipient)) {
            return config.getRecipients().get(recipient);
        }
        if (recipient != null && !recipient.isBlank() && !"default".equals(recipient) && recipient.matches("\\d+")) {
            return recipient;
        }
        return config.getBroadcasterId();
    }

    private String resolveMessageType(Notification notification) {
        Map<String, Object> params = notification.getParams();
        if (params != null) {
            Object typeParam = params.get("messageType");
            if (typeParam instanceof String t && ("bot".equals(t) || "user".equals(t))) {
                return t;
            }
        }
        return config.getMessageType();
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
