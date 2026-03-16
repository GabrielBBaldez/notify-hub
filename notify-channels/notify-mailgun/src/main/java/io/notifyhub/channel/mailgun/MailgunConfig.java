package io.notifyhub.channel.mailgun;

/**
 * Mailgun transactional email configuration.
 *
 * <pre>{@code
 * MailgunConfig config = MailgunConfig.builder()
 *     .apiKey("key-abc123")
 *     .domain("mail.example.com")
 *     .from("noreply@example.com")
 *     .build();
 * }</pre>
 *
 * <p>Region defaults to {@code "US"} (api.mailgun.net). Set to {@code "EU"} to use the
 * European endpoint (api.eu.mailgun.net) for GDPR compliance.</p>
 */
public class MailgunConfig {

    private final String apiKey;
    private final String domain;
    private final String from;
    private final String region;
    private final int timeoutMs;

    private MailgunConfig(Builder builder) {
        this.apiKey = requireNonBlank(builder.apiKey, "Mailgun API key");
        this.domain = requireNonBlank(builder.domain, "Mailgun domain");
        this.from = requireNonBlank(builder.from, "Mailgun from address");
        this.region = builder.region != null && !builder.region.isBlank() ? builder.region.toUpperCase() : "US";
        this.timeoutMs = builder.timeoutMs;
    }

    public String getApiKey() { return apiKey; }
    public String getDomain() { return domain; }
    public String getFrom() { return from; }
    public String getRegion() { return region; }
    public int getTimeoutMs() { return timeoutMs; }

    /**
     * Returns the Mailgun API base URL based on the configured region.
     *
     * @return US endpoint ({@code https://api.mailgun.net}) or EU endpoint
     *         ({@code https://api.eu.mailgun.net})
     */
    public String getBaseUrl() {
        if ("EU".equalsIgnoreCase(region)) {
            return "https://api.eu.mailgun.net";
        }
        return "https://api.mailgun.net";
    }

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
        private String domain;
        private String from;
        private String region = "US";
        private int timeoutMs = 10_000;

        public Builder apiKey(String apiKey) { this.apiKey = apiKey; return this; }
        public Builder domain(String domain) { this.domain = domain; return this; }
        public Builder from(String from) { this.from = from; return this; }
        public Builder region(String region) { this.region = region; return this; }
        public Builder timeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; return this; }

        public MailgunConfig build() {
            return new MailgunConfig(this);
        }
    }
}
