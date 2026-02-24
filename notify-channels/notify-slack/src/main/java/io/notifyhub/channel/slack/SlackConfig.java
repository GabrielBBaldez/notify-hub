package io.notifyhub.channel.slack;

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
    private final int timeoutMs;

    private SlackConfig(Builder builder) {
        this.webhookUrl = requireNonBlank(builder.webhookUrl, "Slack webhook URL");
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

        public SlackConfig build() {
            return new SlackConfig(this);
        }
    }
}
