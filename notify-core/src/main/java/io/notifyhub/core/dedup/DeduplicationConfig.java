package io.notifyhub.core.dedup;

import java.time.Duration;

/**
 * Configuration for notification deduplication.
 *
 * <pre>{@code
 * DeduplicationConfig config = DeduplicationConfig.builder()
 *     .enabled(true)
 *     .ttl(Duration.ofHours(12))
 *     .strategy(DeduplicationConfig.Strategy.BOTH)
 *     .build();
 * }</pre>
 */
public class DeduplicationConfig {

    /**
     * Strategy for generating deduplication keys.
     */
    public enum Strategy {
        /** Generate key from SHA-256 hash of recipient + channel + subject + content. */
        CONTENT_HASH,
        /** Use the explicit key provided via {@code .deduplicationKey("order-123")}. */
        EXPLICIT_KEY,
        /** Use explicit key if provided, otherwise fall back to content hash. */
        BOTH
    }

    private final boolean enabled;
    private final Duration ttl;
    private final Strategy strategy;

    private DeduplicationConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.ttl = builder.ttl;
        this.strategy = builder.strategy;
    }

    public boolean isEnabled() { return enabled; }
    public Duration getTtl() { return ttl; }
    public Strategy getStrategy() { return strategy; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean enabled = true;
        private Duration ttl = Duration.ofHours(24);
        private Strategy strategy = Strategy.CONTENT_HASH;

        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder ttl(Duration ttl) { this.ttl = ttl; return this; }
        public Builder strategy(Strategy strategy) { this.strategy = strategy; return this; }

        public DeduplicationConfig build() {
            return new DeduplicationConfig(this);
        }
    }
}
