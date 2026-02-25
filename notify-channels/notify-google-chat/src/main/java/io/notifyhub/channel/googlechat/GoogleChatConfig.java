package io.notifyhub.channel.googlechat;

/**
 * Google Chat webhook configuration.
 *
 * <p>Google Chat uses Incoming Webhooks to receive messages in Spaces.
 * Create a webhook in your Google Chat Space and provide the URL here.</p>
 *
 * <pre>{@code
 * GoogleChatConfig config = GoogleChatConfig.builder()
 *     .webhookUrl("https://chat.googleapis.com/v1/spaces/XXX/messages?key=YYY&token=ZZZ")
 *     .build();
 * }</pre>
 */
public class GoogleChatConfig {

    private final String webhookUrl;
    private final int timeoutMs;

    private GoogleChatConfig(Builder builder) {
        this.webhookUrl = requireNonBlank(builder.webhookUrl, "Google Chat webhook URL");
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

        public GoogleChatConfig build() {
            return new GoogleChatConfig(this);
        }
    }
}
