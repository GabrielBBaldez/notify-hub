package io.notifyhub.channel.youtube;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * YouTube Live Chat API configuration.
 *
 * <pre>{@code
 * YouTubeConfig config = YouTubeConfig.builder()
 *     .accessToken("ya29.a0...")
 *     .channelId("UC...")
 *     .build();
 * }</pre>
 */
public class YouTubeConfig {

    private final String accessToken;
    private final String channelId;
    private final String liveChatId;
    private final Map<String, String> recipients;
    private final int timeoutMs;

    private YouTubeConfig(Builder builder) {
        this.accessToken = requireNonBlank(builder.accessToken, "YouTube access token");
        this.channelId = builder.channelId;
        this.liveChatId = builder.liveChatId;
        this.recipients = builder.recipients != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(builder.recipients))
                : Collections.emptyMap();
        this.timeoutMs = builder.timeoutMs;
    }

    public String getAccessToken() { return accessToken; }
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
        private String channelId;
        private String liveChatId;
        private Map<String, String> recipients;
        private int timeoutMs = 10_000;

        public Builder accessToken(String accessToken) { this.accessToken = accessToken; return this; }
        public Builder channelId(String channelId) { this.channelId = channelId; return this; }
        public Builder liveChatId(String liveChatId) { this.liveChatId = liveChatId; return this; }
        public Builder recipients(Map<String, String> recipients) { this.recipients = recipients; return this; }
        public Builder timeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; return this; }

        public YouTubeConfig build() {
            return new YouTubeConfig(this);
        }
    }
}
