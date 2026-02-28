package io.notifyhub.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Notification listener that sends HTTP POST webhooks on notification status changes.
 * Fires asynchronously — never blocks the notification flow.
 *
 * <pre>{@code
 * StatusWebhookListener webhook = new StatusWebhookListener(
 *     "https://api.myapp.com/hooks/status",
 *     5000,
 *     Map.of("Authorization", "Bearer token")
 * );
 *
 * NotifyHub notify = NotifyHub.builder()
 *     .channel(emailChannel)
 *     .listener(webhook)
 *     .build();
 * }</pre>
 */
public class StatusWebhookListener implements NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(StatusWebhookListener.class);
    private static final int MAX_RECENT_DELIVERIES = 50;

    private final String url;
    private final int timeoutMs;
    private final Map<String, String> headers;
    private final String signingSecret;
    private final HttpClient httpClient;
    private final List<WebhookDeliveryRecord> recentDeliveries;

    public StatusWebhookListener(String url, int timeoutMs, Map<String, String> headers) {
        this(url, timeoutMs, headers, null);
    }

    public StatusWebhookListener(String url, int timeoutMs, Map<String, String> headers, String signingSecret) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Status webhook URL must not be blank");
        }
        this.url = url;
        this.timeoutMs = timeoutMs > 0 ? timeoutMs : 10_000;
        this.headers = headers != null ? Map.copyOf(headers) : Map.of();
        this.signingSecret = signingSecret;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(this.timeoutMs))
                .build();
        this.recentDeliveries = Collections.synchronizedList(new LinkedList<>());
    }

    @Override
    public void onSuccess(String channelName, String templateName) {
        fireAsync(buildPayload("SENT", channelName, templateName, null, null, null));
    }

    @Override
    public void onFailure(String channelName, String templateName, Exception error) {
        fireAsync(buildPayload("FAILED", channelName, templateName, null,
                error != null ? error.getClass().getSimpleName() : null,
                error != null ? error.getMessage() : null));
    }

    @Override
    public void onScheduled(String channelName, String recipient, Duration delay) {
        fireAsync(buildPayload("SCHEDULED", channelName, null, recipient, null,
                "delay=" + delay.toSeconds() + "s"));
    }

    @Override
    public void onCancelled(String channelName, String recipient) {
        fireAsync(buildPayload("CANCELLED", channelName, null, recipient, null, null));
    }

    /** The configured webhook URL. */
    public String getUrl() { return url; }

    /** Request timeout in milliseconds. */
    public int getTimeoutMs() { return timeoutMs; }

    /** Custom headers sent with each request. */
    public Map<String, String> getHeaders() { return headers; }

    /** Whether HMAC-SHA256 signing is enabled. */
    public boolean isSigningEnabled() { return signingSecret != null && !signingSecret.isBlank(); }

    /** Recent webhook delivery records (newest first, max 50). */
    public List<WebhookDeliveryRecord> getRecentDeliveries() {
        synchronized (recentDeliveries) {
            return List.copyOf(recentDeliveries);
        }
    }

    // ===================== INTERNAL =====================

    private String buildPayload(String event, String channel, String template,
                                String recipient, String errorType, String details) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"event\":\"").append(event).append("\"");
        if (channel != null) {
            sb.append(",\"channel\":\"").append(escapeJson(channel)).append("\"");
        }
        if (recipient != null) {
            sb.append(",\"recipient\":\"").append(escapeJson(recipient)).append("\"");
        }
        if (template != null) {
            sb.append(",\"template\":\"").append(escapeJson(template)).append("\"");
        }
        sb.append(",\"timestamp\":\"").append(Instant.now().toString()).append("\"");
        if (errorType != null) {
            sb.append(",\"error\":{\"type\":\"").append(escapeJson(errorType)).append("\"");
            if (details != null) {
                sb.append(",\"message\":\"").append(escapeJson(details)).append("\"");
            }
            sb.append("}");
        } else if (details != null) {
            sb.append(",\"details\":\"").append(escapeJson(details)).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private void fireAsync(String jsonPayload) {
        CompletableFuture.runAsync(() -> {
            Instant start = Instant.now();
            int statusCode = -1;
            String errorMsg = null;
            try {
                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .timeout(Duration.ofMillis(timeoutMs));
                headers.forEach(reqBuilder::header);

                if (isSigningEnabled()) {
                    String timestamp = String.valueOf(Instant.now().getEpochSecond());
                    String signature = computeHmacSha256(signingSecret, timestamp + "." + jsonPayload);
                    reqBuilder.header("X-NotifyHub-Signature", "sha256=" + signature);
                    reqBuilder.header("X-NotifyHub-Timestamp", timestamp);
                }

                HttpResponse<String> response = httpClient.send(
                        reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
                statusCode = response.statusCode();

                if (statusCode < 200 || statusCode >= 300) {
                    log.warn("Status webhook returned {}: {}", statusCode, response.body());
                }
            } catch (Exception e) {
                errorMsg = e.getMessage();
                log.warn("Status webhook call failed: {}", e.getMessage());
            } finally {
                recordDelivery(jsonPayload, statusCode, errorMsg, start);
            }
        });
    }

    private void recordDelivery(String payload, int statusCode, String error, Instant sentAt) {
        WebhookDeliveryRecord record = new WebhookDeliveryRecord(sentAt, statusCode, error, payload);
        synchronized (recentDeliveries) {
            recentDeliveries.add(0, record);
            while (recentDeliveries.size() > MAX_RECENT_DELIVERIES) {
                recentDeliveries.remove(recentDeliveries.size() - 1);
            }
        }
    }

    static String computeHmacSha256(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : rawHmac) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC-SHA256", e);
        }
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // ===================== DELIVERY RECORD =====================

    /**
     * Record of a single webhook delivery attempt.
     */
    public static class WebhookDeliveryRecord {
        private final Instant sentAt;
        private final int statusCode;
        private final String error;
        private final String payload;

        public WebhookDeliveryRecord(Instant sentAt, int statusCode, String error, String payload) {
            this.sentAt = sentAt;
            this.statusCode = statusCode;
            this.error = error;
            this.payload = payload;
        }

        public Instant getSentAt() { return sentAt; }
        public int getStatusCode() { return statusCode; }
        public String getError() { return error; }
        public String getPayload() { return payload; }
        public boolean isSuccess() { return statusCode >= 200 && statusCode < 300; }
    }
}
