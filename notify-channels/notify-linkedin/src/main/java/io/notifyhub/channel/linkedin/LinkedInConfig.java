package io.notifyhub.channel.linkedin;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LinkedIn API configuration.
 *
 * <pre>{@code
 * LinkedInConfig config = LinkedInConfig.builder()
 *     .accessToken("Bearer-token")
 *     .authorId("urn:li:person:abc123")
 *     .build();
 * }</pre>
 */
public class LinkedInConfig {

    private final String accessToken;
    private final String authorId;
    private final Map<String, String> recipients;
    private final int timeoutMs;

    private LinkedInConfig(Builder builder) {
        this.accessToken = requireNonBlank(builder.accessToken, "LinkedIn access token");
        this.authorId = requireNonBlank(builder.authorId, "LinkedIn author ID");
        this.recipients = builder.recipients != null ? Collections.unmodifiableMap(new LinkedHashMap<>(builder.recipients)) : Collections.emptyMap();
        this.timeoutMs = builder.timeoutMs;
    }

    public String getAccessToken() { return accessToken; }
    public String getAuthorId() { return authorId; }
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
        private String authorId;
        private Map<String, String> recipients;
        private int timeoutMs = 10_000;

        public Builder accessToken(String accessToken) { this.accessToken = accessToken; return this; }
        public Builder authorId(String authorId) { this.authorId = authorId; return this; }
        public Builder recipients(Map<String, String> recipients) { this.recipients = recipients; return this; }
        public Builder timeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; return this; }

        public LinkedInConfig build() {
            return new LinkedInConfig(this);
        }
    }
}
