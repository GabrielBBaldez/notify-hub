package io.notifyhub.channel.template;

/**
 * Configuration for the Template channel.
 *
 * <p>TODO: Replace "Template" with your channel name (e.g., "PagerDuty").</p>
 * <p>TODO: Add fields for your channel's credentials (API key, webhook URL, etc.).</p>
 *
 * <pre>{@code
 * TemplateChannelConfig config = TemplateChannelConfig.builder()
 *     .apiKey("your-api-key")
 *     .build();
 * }</pre>
 */
public class TemplateChannelConfig {

    private final String apiKey; // TODO: Replace with your channel's required credentials

    private TemplateChannelConfig(Builder builder) {
        this.apiKey = builder.apiKey;
    }

    public String getApiKey() {
        return apiKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String apiKey;

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public TemplateChannelConfig build() {
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalArgumentException("apiKey must not be null or blank");
            }
            return new TemplateChannelConfig(this);
        }
    }
}
