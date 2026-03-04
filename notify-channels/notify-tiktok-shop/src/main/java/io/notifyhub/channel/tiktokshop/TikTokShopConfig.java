package io.notifyhub.channel.tiktokshop;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * TikTok Shop API configuration.
 *
 * <pre>{@code
 * TikTokShopConfig config = TikTokShopConfig.builder()
 *     .appKey("your-app-key")
 *     .appSecret("your-app-secret")
 *     .accessToken("your-access-token")
 *     .shopId("optional-shop-id")
 *     .build();
 * }</pre>
 */
public class TikTokShopConfig {

    private final String appKey;
    private final String appSecret;
    private final String accessToken;
    private final String shopId;
    private final Map<String, String> recipients;
    private final int timeoutMs;

    private TikTokShopConfig(Builder builder) {
        this.appKey = requireNonBlank(builder.appKey, "TikTok Shop app key");
        this.appSecret = requireNonBlank(builder.appSecret, "TikTok Shop app secret");
        this.accessToken = requireNonBlank(builder.accessToken, "TikTok Shop access token");
        this.shopId = builder.shopId;
        this.recipients = builder.recipients != null ? Collections.unmodifiableMap(new LinkedHashMap<>(builder.recipients)) : Collections.emptyMap();
        this.timeoutMs = builder.timeoutMs;
    }

    public String getAppKey() { return appKey; }
    public String getAppSecret() { return appSecret; }
    public String getAccessToken() { return accessToken; }
    public String getShopId() { return shopId; }
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
        private String appKey;
        private String appSecret;
        private String accessToken;
        private String shopId;
        private Map<String, String> recipients;
        private int timeoutMs = 10_000;

        public Builder appKey(String appKey) { this.appKey = appKey; return this; }
        public Builder appSecret(String appSecret) { this.appSecret = appSecret; return this; }
        public Builder accessToken(String accessToken) { this.accessToken = accessToken; return this; }
        public Builder shopId(String shopId) { this.shopId = shopId; return this; }
        public Builder recipients(Map<String, String> recipients) { this.recipients = recipients; return this; }
        public Builder timeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; return this; }

        public TikTokShopConfig build() {
            return new TikTokShopConfig(this);
        }
    }
}
