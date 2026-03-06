package io.notifyhub.channel.googlechat;

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
 * Google Chat notification channel using Incoming Webhooks.
 *
 * <p>Sends messages to a Google Chat Space via a webhook URL.
 * No external SDK needed — uses the JDK {@link HttpClient}.</p>
 *
 * <p>Google Chat webhook API expects a JSON payload with a {@code text} field:</p>
 * <pre>{"text": "Hello from NotifyHub!"}</pre>
 *
 * <pre>{@code
 * GoogleChatChannel googleChat = new GoogleChatChannel(
 *     GoogleChatConfig.builder()
 *         .webhookUrl("https://chat.googleapis.com/v1/spaces/XXX/messages?key=YYY&token=ZZZ")
 *         .build()
 * );
 * }</pre>
 */
public class GoogleChatChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(GoogleChatChannel.class);

    private final GoogleChatConfig config;
    private final HttpClient httpClient;

    public GoogleChatChannel(GoogleChatConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();
    }

    @Override
    public String getName() {
        return "google-chat";
    }

    @Override
    public void send(Notification notification) {
        String content = notification.getRenderedContent();
        String imageUrl = notification.getImageUrl();

        try {
            String payload;
            if (imageUrl != null && !imageUrl.isBlank()) {
                payload = "{\"text\": \"" + escapeJson(content) + "\", "
                        + "\"cards\": [{\"sections\": [{\"widgets\": [{\"image\": "
                        + "{\"imageUrl\": \"" + escapeJson(imageUrl) + "\"}}]}]}]}";
            } else {
                payload = String.format("{\"text\": \"%s\"}", escapeJson(content));
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(resolveWebhookUrl(notification.getRecipient())))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new NotificationSendException("google-chat",
                        "Google Chat webhook returned " + response.statusCode() + ": " + response.body());
            }

            log.debug("Google Chat message sent: {}", response.body());

        } catch (NotificationSendException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationSendException("google-chat",
                    "Failed to send Google Chat webhook: " + e.getMessage(), e);
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
