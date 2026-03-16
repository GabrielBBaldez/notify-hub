package io.notifyhub.channel.tiktokshop;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
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
import java.util.Map;

/**
 * TikTok Shop notification channel using TikTok Shop Open API.
 *
 * <p>Sends push messages via {@code POST https://open-api.tiktokglobalshop.com/api/push/message}
 * using HMAC-SHA256 request signing. No external SDK needed.</p>
 */
public class TikTokShopChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(TikTokShopChannel.class);
    private static final String API_BASE = "https://open-api.tiktokglobalshop.com";
    private static final String PUSH_MESSAGE_PATH = "/api/push/message";

    private final TikTokShopConfig config;
    private final HttpClient httpClient;

    public TikTokShopChannel(TikTokShopConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();
    }

    @Override
    public String getName() {
        return "tiktok-shop";
    }

    @Override
    public void send(Notification notification) {
        String content = notification.getRenderedContent();
        String recipient = notification.getRecipient();

        try {
            long timestamp = System.currentTimeMillis() / 1000;

            String sign = generateSign(PUSH_MESSAGE_PATH, timestamp);

            String url = API_BASE + PUSH_MESSAGE_PATH
                    + "?app_key=" + config.getAppKey()
                    + "&access_token=" + config.getAccessToken()
                    + "&timestamp=" + timestamp
                    + "&sign=" + sign;

            String payload = buildPayload(recipient, content);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new NotificationSendException("tiktok-shop",
                        "TikTok Shop API returned " + response.statusCode() + ": " + response.body());
            }

            log.debug("TikTok Shop message sent: {}", response.statusCode());

        } catch (NotificationSendException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationSendException("tiktok-shop",
                    "Failed to send TikTok Shop message: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        return config.getAppKey() != null && !config.getAppKey().isBlank();
    }

    @Override
    public Map<String, String> getConfiguredRecipients() {
        return config.getRecipients();
    }

    /**
     * Generate HMAC-SHA256 signature for TikTok Shop API request.
     *
     * <p>The signing string is: {@code app_secret + path + access_token + app_key + timestamp + app_secret}</p>
     *
     * @param path      the API path (e.g. {@code /api/push/message})
     * @param timestamp Unix timestamp in seconds
     * @return hex-encoded HMAC-SHA256 signature
     */
    String generateSign(String path, long timestamp) {
        String baseString = config.getAppSecret()
                + path
                + config.getAccessToken()
                + config.getAppKey()
                + timestamp
                + config.getAppSecret();

        return hmacSha256(config.getAppSecret(), baseString);
    }

    private static String hmacSha256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : rawHmac) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC-SHA256", e);
        }
    }

    private String buildPayload(String recipient, String content) {
        StringBuilder json = new StringBuilder("{");
        if (recipient != null && !recipient.isBlank()) {
            json.append("\"to\": \"").append(escapeJson(recipient)).append("\", ");
        }
        if (config.getShopId() != null && !config.getShopId().isBlank()) {
            json.append("\"shop_id\": \"").append(escapeJson(config.getShopId())).append("\", ");
        }
        json.append("\"message\": \"").append(escapeJson(content)).append("\"");
        json.append("}");
        return json.toString();
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
