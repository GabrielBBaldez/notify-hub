package io.notifyhub.channel.instagram;

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
 * Instagram notification channel using the Meta Graph API.
 *
 * <p>Supports two modes:</p>
 * <ul>
 *   <li><b>DM (Direct Message)</b> — sends a private message via the Instagram Messaging API.
 *       Use any recipient value (IGSID, alias, or @username).</li>
 *   <li><b>Feed post</b> — publishes a text post to the Instagram feed.
 *       Use {@code "feed"} as the recipient.</li>
 * </ul>
 *
 * <pre>{@code
 * InstagramChannel instagram = new InstagramChannel(
 *     InstagramConfig.builder()
 *         .accessToken("EAAGm0PX4ZCps...")
 *         .igUserId("17841405793...")
 *         .build()
 * );
 * }</pre>
 */
public class InstagramChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(InstagramChannel.class);
    private static final String GRAPH_API = "https://graph.facebook.com/v21.0";

    private final InstagramConfig config;
    private final HttpClient httpClient;

    public InstagramChannel(InstagramConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();
    }

    @Override
    public String getName() {
        return "instagram";
    }

    @Override
    public void send(Notification notification) {
        String content = notification.getRenderedContent();
        String recipient = resolveRecipient(notification.getRecipient());

        if ("feed".equalsIgnoreCase(recipient)) {
            postToFeed(content);
        } else {
            sendDirectMessage(recipient, content);
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

    /**
     * Sends a direct message via the Instagram Messaging API.
     */
    private void sendDirectMessage(String recipientId, String content) {
        try {
            String payload = String.format(
                    "{\"recipient\":{\"id\":\"%s\"},\"message\":{\"text\":\"%s\"}}",
                    escapeJson(recipientId),
                    escapeJson(content)
            );

            String url = GRAPH_API + "/" + config.getIgUserId() + "/messages";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getAccessToken())
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new NotificationSendException("instagram",
                        "Instagram Messaging API returned " + response.statusCode() + ": " + response.body());
            }

            log.debug("Instagram DM sent to '{}': {}", recipientId, response.body());

        } catch (NotificationSendException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationSendException("instagram",
                    "Failed to send Instagram DM: " + e.getMessage(), e);
        }
    }

    /**
     * Publishes a text post to the Instagram feed (two-step: create media container, then publish).
     */
    private void postToFeed(String content) {
        try {
            // Step 1: Create media container
            String createPayload = String.format(
                    "{\"caption\":\"%s\",\"media_type\":\"TEXT\"}",
                    escapeJson(content)
            );

            String createUrl = GRAPH_API + "/" + config.getIgUserId() + "/media";

            HttpRequest createRequest = HttpRequest.newBuilder()
                    .uri(URI.create(createUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getAccessToken())
                    .POST(HttpRequest.BodyPublishers.ofString(createPayload))
                    .build();

            HttpResponse<String> createResponse = httpClient.send(createRequest, HttpResponse.BodyHandlers.ofString());

            if (createResponse.statusCode() != 200) {
                throw new NotificationSendException("instagram",
                        "Instagram Media API returned " + createResponse.statusCode() + ": " + createResponse.body());
            }

            // Extract creation_id from response (simple JSON parsing without external lib)
            String creationId = extractJsonValue(createResponse.body(), "id");
            if (creationId == null) {
                throw new NotificationSendException("instagram",
                        "Instagram Media API did not return a media container ID: " + createResponse.body());
            }

            // Step 2: Publish the media container
            String publishPayload = String.format(
                    "{\"creation_id\":\"%s\"}",
                    escapeJson(creationId)
            );

            String publishUrl = GRAPH_API + "/" + config.getIgUserId() + "/media_publish";

            HttpRequest publishRequest = HttpRequest.newBuilder()
                    .uri(URI.create(publishUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getAccessToken())
                    .POST(HttpRequest.BodyPublishers.ofString(publishPayload))
                    .build();

            HttpResponse<String> publishResponse = httpClient.send(publishRequest, HttpResponse.BodyHandlers.ofString());

            if (publishResponse.statusCode() != 200) {
                throw new NotificationSendException("instagram",
                        "Instagram Publish API returned " + publishResponse.statusCode() + ": " + publishResponse.body());
            }

            log.debug("Instagram feed post published: {}", publishResponse.body());

        } catch (NotificationSendException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationSendException("instagram",
                    "Failed to publish Instagram feed post: " + e.getMessage(), e);
        }
    }

    private String resolveRecipient(String recipient) {
        if (recipient != null && config.getRecipients().containsKey(recipient)) {
            return config.getRecipients().get(recipient);
        }
        if (recipient != null && !recipient.isBlank()) {
            return recipient;
        }
        return "feed";
    }

    /**
     * Extracts a top-level string value from a simple JSON object.
     * Example: {"id":"12345"} → extractJsonValue(json, "id") → "12345"
     */
    static String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIndex = json.indexOf(search);
        if (keyIndex < 0) return null;

        int colonIndex = json.indexOf(':', keyIndex + search.length());
        if (colonIndex < 0) return null;

        int start = json.indexOf('"', colonIndex + 1);
        if (start < 0) return null;

        int end = json.indexOf('"', start + 1);
        if (end < 0) return null;

        return json.substring(start + 1, end);
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
