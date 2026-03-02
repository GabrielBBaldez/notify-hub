package io.notifyhub.channel.twitch;

import io.notifyhub.core.oauth.OAuthTokenManager;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Twitch Helix API configuration.
 *
 * <p>Supports both static access tokens and automatic OAuth 2.0 token refresh.
 * When {@code refreshToken} and {@code clientSecret} are provided (along with
 * the required {@code clientId}), the access token is automatically refreshed
 * before expiration.</p>
 *
 * <pre>{@code
 * // Static token (existing behavior):
 * TwitchConfig config = TwitchConfig.builder()
 *     .clientId("twitch-app-client-id")
 *     .accessToken("oauth-access-token")
 *     .broadcasterId("123456789")
 *     .senderId("987654321")
 *     .build();
 *
 * // Auto-refresh token:
 * TwitchConfig config = TwitchConfig.builder()
 *     .clientId("twitch-app-client-id")
 *     .accessToken("oauth-access-token")     // optional seed token
 *     .refreshToken("refresh-token-value")
 *     .clientSecret("twitch-client-secret")
 *     .broadcasterId("123456789")
 *     .senderId("987654321")
 *     .build();
 * }</pre>
 */
public class TwitchConfig {

    private final String clientId;
    private final String accessToken;
    private final OAuthTokenManager tokenManager;
    private final String broadcasterId;
    private final String senderId;
    private final Map<String, String> recipients;
    private final int timeoutMs;

    private TwitchConfig(Builder builder) {
        this.clientId = requireNonBlank(builder.clientId, "Twitch client ID");
        this.broadcasterId = requireNonBlank(builder.broadcasterId, "Twitch broadcaster ID");
        this.senderId = requireNonBlank(builder.senderId, "Twitch sender ID");

        if (builder.refreshToken != null && builder.clientSecret != null) {
            OAuthTokenManager.Builder tmBuilder = OAuthTokenManager.builder()
                    .tokenEndpoint("https://id.twitch.tv/oauth2/token")
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
            this.accessToken = requireNonBlank(builder.accessToken, "Twitch access token");
        }

        this.recipients = builder.recipients != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(builder.recipients))
                : Collections.emptyMap();
        this.timeoutMs = builder.timeoutMs;
    }

    public String getClientId() { return clientId; }

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

    public String getBroadcasterId() { return broadcasterId; }
    public String getSenderId() { return senderId; }
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
        private String clientId;
        private String accessToken;
        private String refreshToken;
        private String clientSecret;
        private String broadcasterId;
        private String senderId;
        private Map<String, String> recipients;
        private int timeoutMs = 10_000;

        public Builder clientId(String clientId) { this.clientId = clientId; return this; }
        public Builder accessToken(String accessToken) { this.accessToken = accessToken; return this; }
        public Builder refreshToken(String refreshToken) { this.refreshToken = refreshToken; return this; }
        public Builder clientSecret(String clientSecret) { this.clientSecret = clientSecret; return this; }
        public Builder broadcasterId(String broadcasterId) { this.broadcasterId = broadcasterId; return this; }
        public Builder senderId(String senderId) { this.senderId = senderId; return this; }
        public Builder recipients(Map<String, String> recipients) { this.recipients = recipients; return this; }
        public Builder timeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; return this; }

        public TwitchConfig build() {
            return new TwitchConfig(this);
        }
    }
}
