package io.notifyhub.channel.notion;

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
 * Notion notification channel using the Notion API.
 *
 * <p>Creates pages in a Notion database via {@code POST https://api.notion.com/v1/pages}
 * using an Internal Integration Token.</p>
 */
public class NotionChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(NotionChannel.class);
    private static final String PAGES_URL = "https://api.notion.com/v1/pages";

    private final NotionConfig config;
    private final HttpClient httpClient;

    public NotionChannel(NotionConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();
    }

    @Override
    public String getName() {
        return "notion";
    }

    @Override
    public void send(Notification notification) {
        String content = notification.getRenderedContent();
        String databaseId = resolveDatabaseId(notification.getRecipient());
        String title = notification.getSubject() != null ? notification.getSubject() : "Notification";

        try {
            String payload = buildPayload(databaseId, title, content);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PAGES_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Notion-Version", "2022-06-28")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 201) {
                throw new NotificationSendException("notion",
                        "Notion API returned " + response.statusCode() + ": " + response.body());
            }

            log.debug("Notion page created: {}", response.statusCode());

        } catch (NotificationSendException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationSendException("notion",
                    "Failed to create Notion page: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        return config.getApiKey() != null && !config.getApiKey().isBlank();
    }

    @Override
    public Map<String, String> getConfiguredRecipients() {
        return config.getRecipients();
    }

    private String resolveDatabaseId(String recipient) {
        if (recipient != null && config.getRecipients().containsKey(recipient)) {
            return config.getRecipients().get(recipient);
        }
        if (recipient != null && !recipient.isBlank() && !recipient.equals("default")) {
            return recipient;
        }
        return config.getDatabaseId();
    }

    private String buildPayload(String databaseId, String title, String content) {
        return "{" +
                "\"parent\": {\"database_id\": \"" + escapeJson(databaseId) + "\"}, " +
                "\"properties\": {" +
                "\"Name\": {\"title\": [{\"text\": {\"content\": \"" + escapeJson(title) + "\"}}]}" +
                "}, " +
                "\"children\": [{" +
                "\"object\": \"block\", " +
                "\"type\": \"paragraph\", " +
                "\"paragraph\": {\"rich_text\": [{\"text\": {\"content\": \"" + escapeJson(content) + "\"}}]}" +
                "}]" +
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
