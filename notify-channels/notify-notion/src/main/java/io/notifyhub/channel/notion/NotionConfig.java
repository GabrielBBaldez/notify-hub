package io.notifyhub.channel.notion;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Notion API configuration.
 *
 * <pre>{@code
 * NotionConfig config = NotionConfig.builder()
 *     .apiKey("secret_xxx")
 *     .databaseId("abc123")
 *     .build();
 * }</pre>
 */
public class NotionConfig {

    private final String apiKey;
    private final String databaseId;
    private final Map<String, String> recipients;
    private final int timeoutMs;

    private NotionConfig(Builder builder) {
        this.apiKey = requireNonBlank(builder.apiKey, "Notion API key");
        this.databaseId = requireNonBlank(builder.databaseId, "Notion database ID");
        this.recipients = builder.recipients != null ? Collections.unmodifiableMap(new LinkedHashMap<>(builder.recipients)) : Collections.emptyMap();
        this.timeoutMs = builder.timeoutMs;
    }

    public String getApiKey() { return apiKey; }
    public String getDatabaseId() { return databaseId; }
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
        private String databaseId;
        private Map<String, String> recipients;
        private int timeoutMs = 10_000;

        public Builder apiKey(String apiKey) { this.apiKey = apiKey; return this; }
        public Builder databaseId(String databaseId) { this.databaseId = databaseId; return this; }
        public Builder recipients(Map<String, String> recipients) { this.recipients = recipients; return this; }
        public Builder timeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; return this; }

        public NotionConfig build() {
            return new NotionConfig(this);
        }
    }
}
