package io.notifyhub.channel.push.firebase;

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
 * Firebase Cloud Messaging push notification channel using the legacy HTTP API.
 *
 * <p>Sends push notifications via the FCM legacy API ({@code /fcm/send}).
 * No external SDK needed — uses the JDK {@link HttpClient}.</p>
 *
 * <pre>{@code
 * FirebasePushChannel push = new FirebasePushChannel(
 *     FirebasePushConfig.builder()
 *         .serverKey("AAAA...your-server-key...")
 *         .build()
 * );
 * }</pre>
 */
public class FirebasePushChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(FirebasePushChannel.class);
    private static final String FCM_API = "https://fcm.googleapis.com/fcm/send";

    private final FirebasePushConfig config;
    private final HttpClient httpClient;

    public FirebasePushChannel(FirebasePushConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();
    }

    @Override
    public String getName() {
        return "push";
    }

    @Override
    public void send(Notification notification) {
        String token = notification.getRecipient();
        if (token == null || token.isBlank()) {
            throw new NotificationSendException("push",
                    "No device token (recipient) provided for push notification");
        }

        String content = notification.getRenderedContent();
        String subject = notification.getSubject();
        String title = (subject != null && !subject.isBlank()) ? subject : "Notification";

        try {
            String payload = String.format(
                    "{\"to\": \"%s\", \"notification\": {\"title\": \"%s\", \"body\": \"%s\"}}",
                    escapeJson(token),
                    escapeJson(title),
                    escapeJson(content)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(FCM_API))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "key=" + config.getServerKey())
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new NotificationSendException("push",
                        "FCM API returned " + response.statusCode() + ": " + response.body());
            }

            log.debug("FCM push sent to token '{}': {}", token, response.body());

        } catch (NotificationSendException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationSendException("push",
                    "Failed to send FCM push notification: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        return config.getServerKey() != null && !config.getServerKey().isBlank();
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
