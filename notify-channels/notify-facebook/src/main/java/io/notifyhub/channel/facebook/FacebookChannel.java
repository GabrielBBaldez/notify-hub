package io.notifyhub.channel.facebook;

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
 * Facebook notification channel using the Graph API.
 *
 * <p>Supports two modes:</p>
 * <ul>
 *   <li><b>Page feed post</b> — publishes a post to the Facebook Page wall.
 *       Use {@code "feed"} as the recipient (or any value starting with {@code "feed:"}).</li>
 *   <li><b>Messenger message</b> — sends a message via the Page's Messenger.
 *       Use a PSID (Page-Scoped ID) or a configured alias as the recipient.</li>
 * </ul>
 *
 * <pre>{@code
 * FacebookChannel facebook = new FacebookChannel(
 *     FacebookConfig.builder()
 *         .pageAccessToken("EAAGm0PX4ZCps...")
 *         .pageId("102345678901234")
 *         .build()
 * );
 * }</pre>
 */
public class FacebookChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(FacebookChannel.class);
    private static final String GRAPH_API = "https://graph.facebook.com/v19.0";

    private final FacebookConfig config;
    private final HttpClient httpClient;

    public FacebookChannel(FacebookConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();
    }

    @Override
    public String getName() {
        return "facebook";
    }

    @Override
    public void send(Notification notification) {
        String content = notification.getRenderedContent();
        String recipient = resolveRecipient(notification.getRecipient());

        if ("feed".equalsIgnoreCase(recipient) || recipient.toLowerCase().startsWith("feed:")) {
            postToFeed(content);
        } else {
            sendMessengerMessage(recipient, content);
        }
    }

    @Override
    public boolean isAvailable() {
        return config.getPageAccessToken() != null && !config.getPageAccessToken().isBlank();
    }

    @Override
    public Map<String, String> getConfiguredRecipients() {
        return config.getRecipients();
    }

    /**
     * Sends a message via the Facebook Page Messenger (Send API).
     */
    private void sendMessengerMessage(String recipientId, String content) {
        try {
            String payload = String.format(
                    "{\"recipient\":{\"id\":\"%s\"},\"message\":{\"text\":\"%s\"}}",
                    escapeJson(recipientId),
                    escapeJson(content)
            );

            String url = GRAPH_API + "/" + config.getPageId() + "/messages"
                    + "?access_token=" + config.getPageAccessToken();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new NotificationSendException("facebook",
                        "Facebook Messenger API returned " + response.statusCode() + ": " + response.body());
            }

            log.debug("Facebook Messenger message sent to '{}': {}", recipientId, response.body());

        } catch (NotificationSendException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationSendException("facebook",
                    "Failed to send Facebook Messenger message: " + e.getMessage(), e);
        }
    }

    /**
     * Publishes a post to the Facebook Page feed.
     */
    private void postToFeed(String content) {
        try {
            String payload = String.format(
                    "{\"message\":\"%s\"}",
                    escapeJson(content)
            );

            String url = GRAPH_API + "/" + config.getPageId() + "/feed"
                    + "?access_token=" + config.getPageAccessToken();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new NotificationSendException("facebook",
                        "Facebook Feed API returned " + response.statusCode() + ": " + response.body());
            }

            String postId = extractJsonValue(response.body(), "id");
            log.debug("Facebook Page feed post published (id={}): {}", postId, response.body());

        } catch (NotificationSendException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationSendException("facebook",
                    "Failed to publish Facebook Page feed post: " + e.getMessage(), e);
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
     * Example: {"id":"12345"} -> extractJsonValue(json, "id") -> "12345"
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
