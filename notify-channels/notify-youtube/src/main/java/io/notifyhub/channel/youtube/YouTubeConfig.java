package io.notifyhub.channel.youtube;

import io.notifyhub.core.oauth.OAuthTokenManager;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * YouTube Live Chat API configuration.
 *
 * <p>Supports both static access tokens and automatic OAuth 2.0 token refresh.
 * When {@code refreshToken}, {@code clientId}, and {@code clientSecret} are provided,
 * the access token is automatically refreshed before expiration.</p>
 *
 * <pre>{@code
 * // Static token (existing behavior):
 * YouTubeConfig config = YouTubeConfig.builder()
 *     .accessToken("ya29.a0...")
 *     .build();
 *
 * // Auto-refresh token:
 * YouTubeConfig config = YouTubeConfig.builder()
 *     .accessToken("ya29.a0...")        // optional seed token
 *     .refreshToken("1//0eXyz...")
 *     .clientId("123.apps.googleusercontent.com")
 *     .clientSecret("GOCSPX-...")
 *     .build();
 * }</pre>
 */
public class YouTubeConfig {

    private final String accessToken;
    private final OAuthTokenManager tokenManager;
    private final String channelId;
    private final String liveChatId;
    private final Map<String, String> recipients;
    private final int timeoutMs;

    private YouTubeConfig(Builder builder) {
        if (builder.refreshToken != null && builder.clientId != null && builder.clientSecret != null) {
            OAuthTokenManager.Builder tmBuilder = OAuthTokenManager.builder()
                    .tokenEndpoint("https://oauth2.googleapis.com/token")
                    .refreshToken(builder.refreshToken)
                    .clientId(builder.clientId)
                    .clientSecret(builder.clientSecret);
            if (builder.accessToken != null) {
                tmBuilder.initialAccessToken(builder.accessToken);
            }
            this.tokenManager = tmBuilder.build();
            this.accessToken = null;
        } else {
            this.tokenManager = null;
            this.accessToken = requireNonBlank(builder.accessToken, "YouTube access token");
        }
        this.channelId = builder.channelId;
        this.liveChatId = builder.liveChatId;
        this.recipients = builder.recipients != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(builder.recipients))
                : Collections.emptyMap();
        this.timeoutMs = builder.timeoutMs;
    }

    public String getAccessToken() {
        if (tokenManager != null) {
            return tokenManager.getAccessToken();
        }
        return accessToken;
    }

    /** Returns true if this config uses automatic token refresh. */
    public boolean hasTokenRefresh() {
        return tokenManager != null;
    }

    public String getChannelId() { return channelId; }
    public String getLiveChatId() { return liveChatId; }
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
        private String accessToken;
        private String refreshToken;
        private String clientId;
        private String clientSecret;
        private String channelId;
        private String liveChatId;
        private Map<String, String> recipients;
        private int timeoutMs = 10_000;

        public Builder accessToken(String accessToken) { this.accessToken = accessToken; return this; }
        public Builder refreshToken(String refreshToken) { this.refreshToken = refreshToken; return this; }
        public Builder clientId(String clientId) { this.clientId = clientId; return this; }
        public Builder clientSecret(String clientSecret) { this.clientSecret = clientSecret; return this; }
        public Builder channelId(String channelId) { this.channelId = channelId; return this; }
        public Builder liveChatId(String liveChatId) { this.liveChatId = liveChatId; return this; }
        public Builder recipients(Map<String, String> recipients) { this.recipients = recipients; return this; }
        public Builder timeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; return this; }

        public YouTubeConfig build() {
            return new YouTubeConfig(this);
        }
    }
}
