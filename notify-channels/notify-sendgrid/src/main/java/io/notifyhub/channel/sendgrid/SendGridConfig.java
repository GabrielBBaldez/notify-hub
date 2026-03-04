package io.notifyhub.channel.sendgrid;

/**
 * Configuration for the SendGrid email channel.
 *
 * <pre>{@code
 * SendGridConfig config = SendGridConfig.builder()
 *     .apiKey("SG.xxxx")
 *     .from("noreply@myapp.com")
 *     .fromName("MyApp")
 *     .trackOpens(true)
 *     .trackClicks(true)
 *     .build();
 * }</pre>
 */
public class SendGridConfig {

    private final String apiKey;
    private final String from;
    private final String fromName;
    private final boolean trackOpens;
    private final boolean trackClicks;
    private final int timeoutMs;

    private SendGridConfig(Builder builder) {
        if (builder.apiKey == null || builder.apiKey.isBlank()) {
            throw new IllegalArgumentException("SendGrid API key is required");
        }
        if (builder.from == null || builder.from.isBlank()) {
            throw new IllegalArgumentException("From address is required");
        }
        this.apiKey = builder.apiKey;
        this.from = builder.from;
        this.fromName = builder.fromName;
        this.trackOpens = builder.trackOpens;
        this.trackClicks = builder.trackClicks;
        this.timeoutMs = builder.timeoutMs;
    }

    public String getApiKey() { return apiKey; }
    public String getFrom() { return from; }
    public String getFromName() { return fromName; }
    public boolean isTrackOpens() { return trackOpens; }
    public boolean isTrackClicks() { return trackClicks; }
    public int getTimeoutMs() { return timeoutMs; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String apiKey;
        private String from;
        private String fromName;
        private boolean trackOpens = true;
        private boolean trackClicks = true;
        private int timeoutMs = 10_000;

        public Builder apiKey(String apiKey) { this.apiKey = apiKey; return this; }
        public Builder from(String from) { this.from = from; return this; }
        public Builder fromName(String fromName) { this.fromName = fromName; return this; }
        public Builder trackOpens(boolean trackOpens) { this.trackOpens = trackOpens; return this; }
        public Builder trackClicks(boolean trackClicks) { this.trackClicks = trackClicks; return this; }
        public Builder timeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; return this; }

        public SendGridConfig build() {
            return new SendGridConfig(this);
        }
    }
}
