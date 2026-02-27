package io.notifyhub.channel.twitch;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Twitch Helix API configuration.
 *
 * <pre>{@code
 * TwitchConfig config = TwitchConfig.builder()
 *     .clientId("twitch-app-client-id")
 *     .accessToken("oauth-access-token")
 *     .broadcasterId("123456789")
 *     .senderId("987654321")
 *     .build();
 * }</pre>
 */
public class TwitchConfig {

    private final String clientId;
    private final String accessToken;
    private final String broadcasterId;
    private final String senderId;
    private final Map<String, String> recipients;
    private final int timeoutMs;

    private TwitchConfig(Builder builder) {
        this.clientId = requireNonBlank(builder.clientId, "Twitch client ID");
        this.accessToken = requireNonBlank(builder.accessToken, "Twitch access token");
        this.broadcasterId = requireNonBlank(builder.broadcasterId, "Twitch broadcaster ID");
        this.senderId = requireNonBlank(builder.senderId, "Twitch sender ID");
        this.recipients = builder.recipients != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(builder.recipients))
                : Collections.emptyMap();
        this.timeoutMs = builder.timeoutMs;
    }

    public String getClientId() { return clientId; }
    public String getAccessToken() { return accessToken; }
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
        private String broadcasterId;
        private String senderId;
        private Map<String, String> recipients;
        private int timeoutMs = 10_000;

        public Builder clientId(String clientId) { this.clientId = clientId; return this; }
        public Builder accessToken(String accessToken) { this.accessToken = accessToken; return this; }
        public Builder broadcasterId(String broadcasterId) { this.broadcasterId = broadcasterId; return this; }
        public Builder senderId(String senderId) { this.senderId = senderId; return this; }
        public Builder recipients(Map<String, String> recipients) { this.recipients = recipients; return this; }
        public Builder timeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; return this; }

        public TwitchConfig build() {
            return new TwitchConfig(this);
        }
    }
}
