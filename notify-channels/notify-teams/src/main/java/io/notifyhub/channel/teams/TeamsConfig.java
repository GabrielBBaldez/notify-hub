package io.notifyhub.channel.teams;

/**
 * Microsoft Teams webhook configuration.
 *
 * <pre>{@code
 * TeamsConfig config = TeamsConfig.builder()
 *     .webhookUrl("https://outlook.office.com/webhook/XXX/YYY/ZZZ")
 *     .build();
 * }</pre>
 */
public class TeamsConfig {

    private final String webhookUrl;
    private final int timeoutMs;

    private TeamsConfig(Builder builder) {
        this.webhookUrl = requireNonBlank(builder.webhookUrl, "Teams webhook URL");
        this.timeoutMs = builder.timeoutMs;
    }

    public String getWebhookUrl() { return webhookUrl; }
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
        private int timeoutMs = 10_000;

        public Builder webhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; return this; }
        public Builder timeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; return this; }

        public TeamsConfig build() {
            return new TeamsConfig(this);
        }
    }
}
