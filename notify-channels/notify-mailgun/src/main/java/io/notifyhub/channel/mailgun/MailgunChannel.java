package io.notifyhub.channel.mailgun;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Mailgun email notification channel.
 *
 * <p>Sends transactional emails via the Mailgun API using form-encoded POST requests
 * with HTTP Basic authentication. No external SDK required — uses JDK {@link HttpClient}.</p>
 *
 * <pre>{@code
 * MailgunChannel mailgun = new MailgunChannel(
 *     MailgunConfig.builder()
 *         .apiKey("key-abc123")
 *         .domain("mail.example.com")
 *         .from("noreply@example.com")
 *         .build()
 * );
 * }</pre>
 */
public class MailgunChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(MailgunChannel.class);

    private final MailgunConfig config;
    private final HttpClient httpClient;

    public MailgunChannel(MailgunConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();
    }

    /** Package-private constructor for testing — allows injecting a mock HttpClient. */
    MailgunChannel(MailgunConfig config, HttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
    }

    @Override
    public String getName() {
        return "mailgun";
    }

    @Override
    public boolean isAvailable() {
        return config.getApiKey() != null && !config.getApiKey().isBlank()
                && config.getDomain() != null && !config.getDomain().isBlank();
    }

    @Override
    public void send(Notification notification) {
        String recipient = notification.getRecipient();
        String content = notification.getRenderedContent();
        String subject = resolveSubject(notification);

        try {
            String credentials = Base64.getEncoder().encodeToString(
                    ("api:" + config.getApiKey()).getBytes(StandardCharsets.UTF_8));

            String url = config.getBaseUrl() + "/v3/" + config.getDomain() + "/messages";

            String body = "from=" + encode(config.getFrom())
                    + "&to=" + encode(recipient)
                    + "&subject=" + encode(subject)
                    + "&text=" + encode(content);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + credentials)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofMillis(config.getTimeoutMs()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new NotificationSendException("mailgun",
                        "Mailgun API returned " + response.statusCode() + ": " + response.body());
            }

            log.debug("Mailgun email sent to {} (status {})", recipient, response.statusCode());

        } catch (NotificationSendException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationSendException("mailgun",
                    "Failed to send Mailgun email: " + e.getMessage(), e);
        }
    }

    private String resolveSubject(Notification notification) {
        Object subjectParam = notification.getParams().get("subject");
        if (subjectParam != null && !subjectParam.toString().isBlank()) {
            return subjectParam.toString();
        }
        return "Notification";
    }

    private static String encode(String value) {
        return URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8);
    }
}
