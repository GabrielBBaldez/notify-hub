package io.notifyhub.channel.slack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Slack webhook configuration.
 *
 * <pre>{@code
 * SlackConfig config = SlackConfig.builder()
 *     .webhookUrl("https://hooks.slack.com/services/XXX/YYY/ZZZ")
 *     .build();
 * }</pre>
 */
public class SlackConfig {

    private final String webhookUrl;
    private final Map<String, String> recipients;
    private final int timeoutMs;
    private final String username;
    private final String iconUrl;

    private SlackConfig(Builder builder) {
        this.webhookUrl = requireNonBlank(builder.webhookUrl, "Slack webhook URL");
        this.recipients = builder.recipients != null ? Collections.unmodifiableMap(new LinkedHashMap<>(builder.recipients)) : Collections.emptyMap();
        this.timeoutMs = builder.timeoutMs;
        this.username = builder.username;
        this.iconUrl = builder.iconUrl;
    }

    public String getWebhookUrl() { return webhookUrl; }
    public Map<String, String> getRecipients() { return recipients; }
    public int getTimeoutMs() { return timeoutMs; }
    public String getUsername() { return username; }
    public String getIconUrl() { return iconUrl; }

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
        private String webhookUrl;
        private Map<String, String> recipients;
        private int timeoutMs = 10_000;
        private String username;
        private String iconUrl;

        public Builder webhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; return this; }
        public Builder recipients(Map<String, String> recipients) { this.recipients = recipients; return this; }
        public Builder timeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder iconUrl(String iconUrl) { this.iconUrl = iconUrl; return this; }

        public SlackConfig build() {
            return new SlackConfig(this);
        }
    }
}
