package io.notifyhub.channel.push.firebase;

/**
 * Firebase Cloud Messaging (FCM) configuration using the legacy HTTP API.
 *
 * <pre>{@code
 * FirebasePushConfig config = FirebasePushConfig.builder()
 *     .serverKey("AAAA...your-server-key...")
 *     .build();
 * }</pre>
 */
public class FirebasePushConfig {

    private final String serverKey;
    private final int timeoutMs;

    private FirebasePushConfig(Builder builder) {
        this.serverKey = requireNonBlank(builder.serverKey, "FCM server key");
        this.timeoutMs = builder.timeoutMs;
    }

    public String getServerKey() { return serverKey; }
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
        private String serverKey;
        private int timeoutMs = 10_000;

        public Builder serverKey(String serverKey) { this.serverKey = serverKey; return this; }
        public Builder timeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; return this; }

        public FirebasePushConfig build() {
            return new FirebasePushConfig(this);
        }
    }
}
