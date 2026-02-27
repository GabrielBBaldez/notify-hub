package io.notifyhub.channel.twitter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Twitter/X API v2 configuration.
 *
 * <pre>{@code
 * TwitterConfig config = TwitterConfig.builder()
 *     .apiKey("consumer-key")
 *     .apiSecret("consumer-secret")
 *     .accessToken("access-token")
 *     .accessTokenSecret("access-token-secret")
 *     .build();
 * }</pre>
 */
public class TwitterConfig {

    private final String apiKey;
    private final String apiSecret;
    private final String accessToken;
    private final String accessTokenSecret;
    private final Map<String, String> recipients;
    private final int timeoutMs;

    private TwitterConfig(Builder builder) {
        this.apiKey = requireNonBlank(builder.apiKey, "Twitter API key");
        this.apiSecret = requireNonBlank(builder.apiSecret, "Twitter API secret");
        this.accessToken = requireNonBlank(builder.accessToken, "Twitter access token");
        this.accessTokenSecret = requireNonBlank(builder.accessTokenSecret, "Twitter access token secret");
        this.recipients = builder.recipients != null ? Collections.unmodifiableMap(new LinkedHashMap<>(builder.recipients)) : Collections.emptyMap();
        this.timeoutMs = builder.timeoutMs;
    }

    public String getApiKey() { return apiKey; }
    public String getApiSecret() { return apiSecret; }
    public String getAccessToken() { return accessToken; }
    public String getAccessTokenSecret() { return accessTokenSecret; }
    public Map<String, String> getRecipients() { return recipients; }
    public int getTimeoutMs() { return timeoutMs; }

    public static Builder builder() {
        return new Builder();
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be null or blank");
        }
        return value;
    }

    public static class Builder {
        private String apiKey;
        private String apiSecret;
        private String accessToken;
        private String accessTokenSecret;
        private Map<String, String> recipients;
        private int timeoutMs = 10_000;

        public Builder apiKey(String apiKey) { this.apiKey = apiKey; return this; }
        public Builder apiSecret(String apiSecret) { this.apiSecret = apiSecret; return this; }
        public Builder accessToken(String accessToken) { this.accessToken = accessToken; return this; }
        public Builder accessTokenSecret(String accessTokenSecret) { this.accessTokenSecret = accessTokenSecret; return this; }
        public Builder recipients(Map<String, String> recipients) { this.recipients = recipients; return this; }
        public Builder timeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; return this; }

        public TwitterConfig build() {
            return new TwitterConfig(this);
        }
    }
}
