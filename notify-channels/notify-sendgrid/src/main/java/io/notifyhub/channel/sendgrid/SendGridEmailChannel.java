package io.notifyhub.channel.sendgrid;

import io.notifyhub.core.DeliveryStatus;
import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.EmailStatusProvider;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
import io.notifyhub.core.channel.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

/**
 * SendGrid API email channel with delivery tracking support.
 *
 * <p>Sends emails via SendGrid v3 REST API (not SMTP) and supports
 * querying delivery status (delivered, opened, bounced, etc.) via
 * the Email Activity Feed API.</p>
 *
 * <pre>{@code
 * SendGridEmailChannel channel = new SendGridEmailChannel(
 *     SendGridConfig.builder()
 *         .apiKey("SG.xxxx")
 *         .from("noreply@myapp.com")
 *         .trackOpens(true)
 *         .build()
 * );
 *
 * // Send
 * SendResult result = channel.sendWithResult(notification);
 * String messageId = result.getProviderMessageId();
 *
 * // Later — check status
 * DeliveryStatus status = channel.checkStatus(messageId);
 * // → DELIVERED, OPENED, BOUNCED, SPAM_COMPLAINT, etc.
 * }</pre>
 */
public class SendGridEmailChannel implements NotificationChannel, EmailStatusProvider {

    private static final Logger log = LoggerFactory.getLogger(SendGridEmailChannel.class);

    private static final String SEND_URL = "https://api.sendgrid.com/v3/mail/send";
    private static final String MESSAGES_URL = "https://api.sendgrid.com/v3/messages";

    private final SendGridConfig config;
    private final HttpClient httpClient;

    public SendGridEmailChannel(SendGridConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    // Visible for testing
    SendGridEmailChannel(SendGridConfig config, HttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
    }

    @Override
    public String getName() {
        return "email";
    }

    @Override
    public void send(Notification notification) {
        sendWithResult(notification);
    }

    @Override
    public SendResult sendWithResult(Notification notification) {
        String json = buildSendPayload(notification);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SEND_URL))
                .header("Authorization", "Bearer " + config.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 202 && response.statusCode() != 200) {
                throw new NotificationSendException("email",
                        "SendGrid API returned " + response.statusCode() + ": " + response.body());
            }

            // Extract X-Message-Id header for tracking
            String messageId = response.headers()
                    .firstValue("X-Message-Id").orElse(null);

            log.debug("Email sent via SendGrid to '{}' (messageId: {})",
                    notification.getRecipient(), messageId);

            return messageId != null ? new SendResult(messageId) : null;

        } catch (NotificationSendException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationSendException("email",
                    "SendGrid send failed: " + e.getMessage(), e);
        }
    }

    @Override
    public DeliveryStatus checkStatus(String providerMessageId) {
        if (providerMessageId == null || providerMessageId.isBlank()) {
            throw new NotificationSendException("email", "Provider message ID is required for status check");
        }

        // GET https://api.sendgrid.com/v3/messages?query=msg_id={id}&limit=1
        String url = MESSAGES_URL + "?query=msg_id%3D" + providerMessageId + "&limit=1";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + config.getApiKey())
                .header("Content-Type", "application/json")
                .GET()
                .timeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("SendGrid messages API returned {}: {}",
                        response.statusCode(), response.body());
                return DeliveryStatus.SENT; // Fallback to SENT if API is unavailable
            }

            return parseStatusFromResponse(response.body());

        } catch (Exception e) {
            log.warn("Failed to check email status: {}", e.getMessage());
            return DeliveryStatus.SENT; // Fallback — don't throw on status check failure
        }
    }

    @Override
    public boolean isAvailable() {
        return config.getApiKey() != null && !config.getApiKey().isBlank();
    }

    @Override
    public Map<String, String> getConfiguredRecipients() {
        return Collections.emptyMap();
    }

    // ===================== PRIVATE HELPERS =====================

    /**
     * Build the SendGrid v3 mail/send JSON payload.
     * Manual JSON construction — no Jackson dependency in channel modules.
     */
    private String buildSendPayload(Notification notification) {
        String content = notification.getRenderedContent() != null
                ? notification.getRenderedContent()
                : (notification.getRawContent() != null ? notification.getRawContent() : "");

        boolean isHtml = content.contains("<") && content.contains(">");
        String contentType = isHtml ? "text/html" : "text/plain";

        String subject = notification.getSubject() != null
                ? notification.getSubject()
                : "Notification";

        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // Personalizations (to)
        sb.append("\"personalizations\":[{\"to\":[{\"email\":\"")
          .append(escapeJson(notification.getRecipient()))
          .append("\"}]}],");

        // From
        sb.append("\"from\":{\"email\":\"").append(escapeJson(config.getFrom())).append("\"");
        if (config.getFromName() != null && !config.getFromName().isBlank()) {
            sb.append(",\"name\":\"").append(escapeJson(config.getFromName())).append("\"");
        }
        sb.append("},");

        // Subject
        sb.append("\"subject\":\"").append(escapeJson(subject)).append("\",");

        // Content
        sb.append("\"content\":[{\"type\":\"").append(contentType)
          .append("\",\"value\":\"").append(escapeJson(content)).append("\"}]");

        // Tracking settings
        sb.append(",\"tracking_settings\":{");
        sb.append("\"open_tracking\":{\"enable\":").append(config.isTrackOpens()).append("}");
        sb.append(",\"click_tracking\":{\"enable\":").append(config.isTrackClicks()).append("}");
        sb.append("}");

        sb.append("}");
        return sb.toString();
    }

    /**
     * Parse the SendGrid messages API response to extract the latest status.
     * Response format: {"messages":[{"status":"delivered", ...}]}
     */
    private DeliveryStatus parseStatusFromResponse(String responseBody) {
        // Simple JSON parsing — extract "status" field from first message
        String status = extractJsonField(responseBody, "status");
        if (status == null) {
            return DeliveryStatus.SENT; // No status info yet
        }
        return mapSendGridStatus(status.toLowerCase());
    }

    /**
     * Map SendGrid event/status names to DeliveryStatus enum values.
     */
    static DeliveryStatus mapSendGridStatus(String sgStatus) {
        return switch (sgStatus) {
            case "delivered" -> DeliveryStatus.DELIVERED;
            case "open", "opened" -> DeliveryStatus.OPENED;
            case "click", "clicked" -> DeliveryStatus.CLICKED;
            case "bounce", "bounced" -> DeliveryStatus.BOUNCED;
            case "spamreport", "spam_report", "spam" -> DeliveryStatus.SPAM_COMPLAINT;
            case "dropped", "drop" -> DeliveryStatus.DROPPED;
            case "deferred" -> DeliveryStatus.DEFERRED;
            case "processed" -> DeliveryStatus.SENT;
            case "not_delivered" -> DeliveryStatus.FAILED;
            default -> DeliveryStatus.SENT;
        };
    }

    /**
     * Extract a simple string field value from JSON.
     * Handles: "fieldName":"value" patterns.
     */
    private String extractJsonField(String json, String fieldName) {
        String pattern = "\"" + fieldName + "\":\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return null;
        int start = idx + pattern.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return null;
        return json.substring(start, end);
    }

    /**
     * Escape special characters for JSON string values.
     */
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
