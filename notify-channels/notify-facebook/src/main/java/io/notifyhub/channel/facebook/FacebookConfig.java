package io.notifyhub.channel.facebook;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Facebook Graph API configuration.
 *
 * <pre>{@code
 * FacebookConfig config = FacebookConfig.builder()
 *     .pageAccessToken("EAAGm0PX4ZCps...")
 *     .pageId("102345678901234")
 *     .build();
 * }</pre>
 */
public class FacebookConfig {

    private final String pageAccessToken;
    private final String pageId;
    private final Map<String, String> recipients;
    private final int timeoutMs;

    private FacebookConfig(Builder builder) {
        this.pageAccessToken = requireNonBlank(builder.pageAccessToken, "Facebook page access token");
        this.pageId = requireNonBlank(builder.pageId, "Facebook page ID");
        this.recipients = builder.recipients != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(builder.recipients))
                : Collections.emptyMap();
        this.timeoutMs = builder.timeoutMs;
    }

    public String getPageAccessToken() { return pageAccessToken; }
    public String getPageId() { return pageId; }
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
        private String pageAccessToken;
        private String pageId;
        private Map<String, String> recipients;
        private int timeoutMs = 10_000;

        public Builder pageAccessToken(String pageAccessToken) { this.pageAccessToken = pageAccessToken; return this; }
        public Builder pageId(String pageId) { this.pageId = pageId; return this; }
        public Builder recipients(Map<String, String> recipients) { this.recipients = recipients; return this; }
        public Builder timeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; return this; }

        public FacebookConfig build() {
            return new FacebookConfig(this);
        }
    }
}
