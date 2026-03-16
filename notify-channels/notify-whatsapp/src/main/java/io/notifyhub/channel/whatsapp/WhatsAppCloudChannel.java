package io.notifyhub.channel.whatsapp;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.SendResult;
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
 * WhatsApp notification channel using Meta's Cloud API (Graph API).
 *
 * <p>Sends messages directly via the WhatsApp Business Platform without
 * requiring Twilio as an intermediary. Supports text, image, video, and
 * document messages.</p>
 *
 * <pre>{@code
 * WhatsAppCloudChannel whatsapp = new WhatsAppCloudChannel(
 *     WhatsAppCloudConfig.builder()
 *         .accessToken("EAAGm0PX4ZCps...")
 *         .phoneNumberId("106540012345678")
 *         .build()
 * );
 * }</pre>
 */
public class WhatsAppCloudChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppCloudChannel.class);
    private static final String GRAPH_API_BASE = "https://graph.facebook.com";

    private final WhatsAppCloudConfig config;
    private final HttpClient httpClient;

    public WhatsAppCloudChannel(WhatsAppCloudConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();
    }

    @Override
    public String getName() {
        return "whatsapp";
    }

    @Override
    public void send(Notification notification) {
        String content = notification.getRenderedContent();
        String recipient = resolveRecipient(notification.getRecipient());
        String phone = normalizePhone(recipient);

        // WhatsApp has a 4096 char limit
        if (content.length() > 4096) {
            log.warn("WhatsApp content truncated from {} to 4096 chars", content.length());
            content = content.substring(0, 4093) + "...";
        }

        Object mediaUrl = notification.getParams().get("mediaUrl");
        String payload;

        if (mediaUrl != null && !mediaUrl.toString().isEmpty()) {
            String url = mediaUrl.toString();
            String mediaType = detectMediaType(url, notification.getParams());
            payload = buildMediaPayload(phone, content, url, mediaType);
            log.debug("WhatsApp Cloud API sending {} to '{}' with media: {}", mediaType, phone, url);
        } else {
            payload = buildTextPayload(phone, content);
        }

        String responseBody = sendRequest(payload);
        String messageId = extractMessageId(responseBody);
        log.debug("WhatsApp Cloud API sent to '{}', wamid: {}", phone, messageId);
    }

    @Override
    public SendResult sendWithResult(Notification notification) {
        String content = notification.getRenderedContent();
        String recipient = resolveRecipient(notification.getRecipient());
        String phone = normalizePhone(recipient);

        if (content.length() > 4096) {
            log.warn("WhatsApp content truncated from {} to 4096 chars", content.length());
            content = content.substring(0, 4093) + "...";
        }

        Object mediaUrl = notification.getParams().get("mediaUrl");
        String payload;

        if (mediaUrl != null && !mediaUrl.toString().isEmpty()) {
            String url = mediaUrl.toString();
            String mediaType = detectMediaType(url, notification.getParams());
            payload = buildMediaPayload(phone, content, url, mediaType);
        } else {
            payload = buildTextPayload(phone, content);
        }

        String responseBody = sendRequest(payload);
        String messageId = extractMessageId(responseBody);
        return new SendResult(messageId);
    }

    @Override
    public boolean isAvailable() {
        return config.getAccessToken() != null && !config.getAccessToken().isBlank();
    }

    @Override
    public Map<String, String> getConfiguredRecipients() {
        return config.getRecipients();
    }

    private String sendRequest(String payload) {
        try {
            String url = GRAPH_API_BASE + "/" + config.getApiVersion()
                    + "/" + config.getPhoneNumberId() + "/messages";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getAccessToken())
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new NotificationSendException("whatsapp",
                        "WhatsApp Cloud API returned " + response.statusCode() + ": " + response.body());
            }

            return response.body();

        } catch (NotificationSendException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationSendException("whatsapp",
                    "Failed to send WhatsApp message via Cloud API: " + e.getMessage(), e);
        }
    }

    private String buildTextPayload(String phone, String content) {
        return String.format(
                "{\"messaging_product\":\"whatsapp\",\"to\":\"%s\",\"type\":\"text\",\"text\":{\"body\":\"%s\"}}",
                escapeJson(phone),
                escapeJson(content)
        );
    }

    private String buildMediaPayload(String phone, String caption, String mediaUrl, String mediaType) {
        return String.format(
                "{\"messaging_product\":\"whatsapp\",\"to\":\"%s\",\"type\":\"%s\",\"%s\":{\"link\":\"%s\",\"caption\":\"%s\"}}",
                escapeJson(phone),
                mediaType,
                mediaType,
                escapeJson(mediaUrl),
                escapeJson(caption)
        );
    }

    /**
     * Detects media type from URL extension or explicit mediaType param.
     * Returns "image", "video", or "document".
     */
    static String detectMediaType(String url, Map<String, Object> params) {
        // Check explicit param first
        Object explicitType = params.get("mediaType");
        if (explicitType != null) {
            String type = explicitType.toString().toLowerCase();
            if ("image".equals(type) || "video".equals(type) || "document".equals(type)) {
                return type;
            }
        }

        // Detect from URL extension
        String lower = url.toLowerCase();
        // Strip query params for extension detection
        int queryIndex = lower.indexOf('?');
        if (queryIndex > 0) {
            lower = lower.substring(0, queryIndex);
        }

        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")) {
            return "image";
        }
        if (lower.endsWith(".mp4") || lower.endsWith(".3gp")) {
            return "video";
        }

        return "document";
    }

    /**
     * Normalizes phone number: strips "whatsapp:" prefix and ensures digits only (no +).
     * WhatsApp Cloud API expects phone without + prefix.
     */
    static String normalizePhone(String phone) {
        if (phone == null) return "";
        String normalized = phone.trim();
        if (normalized.startsWith("whatsapp:")) {
            normalized = normalized.substring(9);
        }
        if (normalized.startsWith("+")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private String resolveRecipient(String recipient) {
        if (recipient != null && config.getRecipients().containsKey(recipient)) {
            return config.getRecipients().get(recipient);
        }
        return recipient;
    }

    /**
     * Extracts the WhatsApp message ID (wamid) from the Cloud API response.
     * Response format: {"messaging_product":"whatsapp","contacts":[...],"messages":[{"id":"wamid.xxx"}]}
     */
    static String extractMessageId(String json) {
        if (json == null) return null;
        // Find "messages" array, then extract "id" after it
        int messagesIndex = json.indexOf("\"messages\"");
        if (messagesIndex < 0) return null;

        int idIndex = json.indexOf("\"id\"", messagesIndex);
        if (idIndex < 0) return null;

        int colonIndex = json.indexOf(':', idIndex + 4);
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
