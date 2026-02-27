package io.notifyhub.channel.twitter;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Twitter/X notification channel using API v2.
 *
 * <p>Posts tweets via {@code POST https://api.x.com/2/tweets} using
 * OAuth 1.0a authentication. No external SDK needed.</p>
 */
public class TwitterChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(TwitterChannel.class);
    private static final String TWEETS_URL = "https://api.x.com/2/tweets";

    private final TwitterConfig config;
    private final HttpClient httpClient;

    public TwitterChannel(TwitterConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();
    }

    @Override
    public String getName() {
        return "twitter";
    }

    @Override
    public void send(Notification notification) {
        String content = notification.getRenderedContent();

        try {
            String payload = "{\"text\": \"" + escapeJson(content) + "\"}";

            String authHeader = buildOAuthHeader("POST", TWEETS_URL);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TWEETS_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", authHeader)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 201 && response.statusCode() != 200) {
                throw new NotificationSendException("twitter",
                        "Twitter API returned " + response.statusCode() + ": " + response.body());
            }

            log.debug("Tweet posted: {}", response.statusCode());

        } catch (NotificationSendException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationSendException("twitter",
                    "Failed to post tweet: " + e.getMessage(), e);
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

    /**
     * Build OAuth 1.0a Authorization header (HMAC-SHA1).
     */
    String buildOAuthHeader(String method, String url) {
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);

        Map<String, String> oauthParams = new TreeMap<>();
        oauthParams.put("oauth_consumer_key", config.getApiKey());
        oauthParams.put("oauth_nonce", nonce);
        oauthParams.put("oauth_signature_method", "HMAC-SHA1");
        oauthParams.put("oauth_timestamp", timestamp);
        oauthParams.put("oauth_token", config.getAccessToken());
        oauthParams.put("oauth_version", "1.0");

        StringBuilder paramString = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : oauthParams.entrySet()) {
            if (!first) paramString.append("&");
            paramString.append(percentEncode(entry.getKey()))
                       .append("=")
                       .append(percentEncode(entry.getValue()));
            first = false;
        }

        String baseString = method.toUpperCase() + "&"
                + percentEncode(url) + "&"
                + percentEncode(paramString.toString());

        String signingKey = percentEncode(config.getApiSecret()) + "&"
                + percentEncode(config.getAccessTokenSecret());

        String signature = hmacSha1(signingKey, baseString);
        oauthParams.put("oauth_signature", signature);

        StringBuilder header = new StringBuilder("OAuth ");
        first = true;
        for (Map.Entry<String, String> entry : oauthParams.entrySet()) {
            if (!first) header.append(", ");
            header.append(percentEncode(entry.getKey()))
                  .append("=\"")
                  .append(percentEncode(entry.getValue()))
                  .append("\"");
            first = false;
        }

        return header.toString();
    }

    private static String hmacSha1(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC-SHA1", e);
        }
    }

    private static String percentEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
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
