package io.notifyhub.channel.kick;

import io.notifyhub.core.oauth.OAuthTokenManager;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class KickConfig {

    private final String clientId;
    private final String accessToken;
    private final OAuthTokenManager tokenManager;
    private final String broadcasterId;
    private final String messageType;
    private final Map<String, String> recipients;
    private final int timeoutMs;

    private KickConfig(Builder builder) {
        this.clientId = requireNonBlank(builder.clientId, "Kick client ID");
        this.broadcasterId = requireNonBlank(builder.broadcasterId, "Kick broadcaster ID");

        String type = builder.messageType != null ? builder.messageType : "bot";
        if (!"bot".equals(type) && !"user".equals(type)) {
            throw new IllegalArgumentException("Kick messageType must be 'bot' or 'user', got: " + type);
        }
        this.messageType = type;

        if (builder.refreshToken != null && builder.clientSecret != null) {
            OAuthTokenManager.Builder tmBuilder = OAuthTokenManager.builder()
                    .tokenEndpoint("https://id.kick.com/oauth/token")
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
            this.accessToken = requireNonBlank(builder.accessToken, "Kick access token");
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

    public boolean hasTokenRefresh() {
        return tokenManager != null;
    }

    public String getBroadcasterId() { return broadcasterId; }
    public String getMessageType() { return messageType; }
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
        private String messageType;
        private Map<String, String> recipients;
        private int timeoutMs = 10_000;

        public Builder clientId(String clientId) { this.clientId = clientId; return this; }
        public Builder accessToken(String accessToken) { this.accessToken = accessToken; return this; }
        public Builder refreshToken(String refreshToken) { this.refreshToken = refreshToken; return this; }
        public Builder clientSecret(String clientSecret) { this.clientSecret = clientSecret; return this; }
        public Builder broadcasterId(String broadcasterId) { this.broadcasterId = broadcasterId; return this; }
        public Builder messageType(String messageType) { this.messageType = messageType; return this; }
        public Builder recipients(Map<String, String> recipients) { this.recipients = recipients; return this; }
        public Builder timeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; return this; }

        public KickConfig build() {
            return new KickConfig(this);
        }
    }
}
