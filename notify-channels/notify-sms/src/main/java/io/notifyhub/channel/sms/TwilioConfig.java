package io.notifyhub.channel.sms;

/**
 * Twilio API configuration for SMS and WhatsApp channels.
 *
 * <pre>{@code
 * TwilioConfig config = TwilioConfig.builder()
 *     .accountSid("ACXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX")
 *     .authToken("your-auth-token")
 *     .fromNumber("+5548999999999")
 *     .build();
 * }</pre>
 */
public class TwilioConfig {

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;

    private TwilioConfig(Builder builder) {
        this.accountSid = requireNonBlank(builder.accountSid, "Twilio Account SID");
        this.authToken = requireNonBlank(builder.authToken, "Twilio Auth Token");
        this.fromNumber = requireNonBlank(builder.fromNumber, "From number");
    }

    public String getAccountSid() { return accountSid; }
    public String getAuthToken() { return authToken; }
    public String getFromNumber() { return fromNumber; }

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
        private String accountSid;
        private String authToken;
        private String fromNumber;

        public Builder accountSid(String accountSid) { this.accountSid = accountSid; return this; }
        public Builder authToken(String authToken) { this.authToken = authToken; return this; }
        public Builder fromNumber(String fromNumber) { this.fromNumber = fromNumber; return this; }

        public TwilioConfig build() {
            return new TwilioConfig(this);
        }
    }
}
