package io.notifyhub.channel.discord;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Discord webhook configuration.
 *
 * <pre>{@code
 * DiscordConfig config = DiscordConfig.builder()
 *     .webhookUrl("https://discord.com/api/webhooks/123/abc")
 *     .username("NotifyHub Bot")
 *     .build();
 * }</pre>
 */
public class DiscordConfig {

    private final String webhookUrl;
    private final String username;
    private final String avatarUrl;
    private final Map<String, String> recipients;
    private final int timeoutMs;

    private DiscordConfig(Builder builder) {
        this.webhookUrl = requireNonBlank(builder.webhookUrl, "Discord webhook URL");
        this.username = builder.username;
        this.avatarUrl = builder.avatarUrl;
        this.recipients = builder.recipients != null ? Collections.unmodifiableMap(new LinkedHashMap<>(builder.recipients)) : Collections.emptyMap();
        this.timeoutMs = builder.timeoutMs;
    }

    public String getWebhookUrl() { return webhookUrl; }
    public String getUsername() { return username; }
    public String getAvatarUrl() { return avatarUrl; }
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
        private String webhookUrl;
        private String username;
        private String avatarUrl;
        private Map<String, String> recipients;
        private int timeoutMs = 10_000;

        public Builder webhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder avatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; return this; }
        public Builder recipients(Map<String, String> recipients) { this.recipients = recipients; return this; }
        public Builder timeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; return this; }

        public DiscordConfig build() {
            return new DiscordConfig(this);
        }
    }
}
