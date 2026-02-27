package io.notifyhub.channel.linkedin;

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
 * LinkedIn notification channel using the REST API.
 *
 * <p>Publishes posts via {@code POST https://api.linkedin.com/rest/posts}
 * using OAuth 2.0 Bearer token authentication.</p>
 */
public class LinkedInChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(LinkedInChannel.class);
    private static final String POSTS_URL = "https://api.linkedin.com/rest/posts";

    private final LinkedInConfig config;
    private final HttpClient httpClient;

    public LinkedInChannel(LinkedInConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();
    }

    @Override
    public String getName() {
        return "linkedin";
    }

    @Override
    public void send(Notification notification) {
        String content = notification.getRenderedContent();
        String authorId = resolveAuthorId(notification.getRecipient());

        try {
            String payload = buildPayload(authorId, content);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(POSTS_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getAccessToken())
                    .header("LinkedIn-Version", "202401")
                    .header("X-Restli-Protocol-Version", "2.0.0")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 201 && response.statusCode() != 200) {
                throw new NotificationSendException("linkedin",
                        "LinkedIn API returned " + response.statusCode() + ": " + response.body());
            }

            log.debug("LinkedIn post published: {}", response.statusCode());

        } catch (NotificationSendException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationSendException("linkedin",
                    "Failed to publish LinkedIn post: " + e.getMessage(), e);
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

    private String resolveAuthorId(String recipient) {
        if (recipient != null && config.getRecipients().containsKey(recipient)) {
            return config.getRecipients().get(recipient);
        }
        if (recipient != null && recipient.startsWith("urn:li:")) {
            return recipient;
        }
        return config.getAuthorId();
    }

    private String buildPayload(String authorId, String content) {
        return "{" +
                "\"author\": \"" + escapeJson(authorId) + "\", " +
                "\"lifecycleState\": \"PUBLISHED\", " +
                "\"visibility\": \"PUBLIC\", " +
                "\"commentary\": \"" + escapeJson(content) + "\", " +
                "\"distribution\": {\"feedDistribution\": \"MAIN_FEED\"}" +
                "}";
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
