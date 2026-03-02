package io.notifyhub.channel.instagram;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Instagram Graph API configuration.
 *
 * <pre>{@code
 * InstagramConfig config = InstagramConfig.builder()
 *     .accessToken("EAAGm0PX4ZCps...")
 *     .igUserId("17841405793...")
 *     .build();
 * }</pre>
 */
public class InstagramConfig {

    private final String accessToken;
    private final String igUserId;
    private final Map<String, String> recipients;
    private final int timeoutMs;

    private InstagramConfig(Builder builder) {
        this.accessToken = requireNonBlank(builder.accessToken, "Instagram access token");
        this.igUserId = requireNonBlank(builder.igUserId, "Instagram user ID (ig-user-id)");
        this.recipients = builder.recipients != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(builder.recipients))
                : Collections.emptyMap();
        this.timeoutMs = builder.timeoutMs;
    }

    public String getAccessToken() { return accessToken; }
    public String getIgUserId() { return igUserId; }
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
        private String igUserId;
        private Map<String, String> recipients;
        private int timeoutMs = 10_000;

        public Builder accessToken(String accessToken) { this.accessToken = accessToken; return this; }
        public Builder igUserId(String igUserId) { this.igUserId = igUserId; return this; }
        public Builder recipients(Map<String, String> recipients) { this.recipients = recipients; return this; }
        public Builder timeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; return this; }

        public InstagramConfig build() {
            return new InstagramConfig(this);
        }
    }
}
