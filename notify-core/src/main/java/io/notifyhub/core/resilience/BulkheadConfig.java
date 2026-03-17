package io.notifyhub.core.resilience;

/**
 * Per-channel concurrency limiter configuration.
 *
 * <p>Limits the number of concurrent notification sends per channel,
 * preventing any single channel from monopolizing thread resources.
 *
 * <pre>{@code
 * NotifyHub notify = NotifyHub.builder()
 *     .bulkhead(BulkheadConfig.perChannel(5))
 *     .build();
 * }</pre>
 */
public class BulkheadConfig {

    private final int maxConcurrentCalls;

    private BulkheadConfig(int maxConcurrentCalls) {
        if (maxConcurrentCalls <= 0) {
            throw new IllegalArgumentException(
                    "maxConcurrentCalls must be greater than 0, got: " + maxConcurrentCalls);
        }
        this.maxConcurrentCalls = maxConcurrentCalls;
    }

    /**
     * Create a BulkheadConfig with a custom max concurrent calls per channel.
     *
     * @param max maximum concurrent calls (must be > 0)
     * @return configured BulkheadConfig
     * @throws IllegalArgumentException if max <= 0
     */
    public static BulkheadConfig perChannel(int max) {
        return new BulkheadConfig(max);
    }

    /**
     * Create a BulkheadConfig with the default of 10 concurrent calls per channel.
     *
     * @return default BulkheadConfig
     */
    public static BulkheadConfig defaults() {
        return new BulkheadConfig(10);
    }

    /**
     * Get the maximum number of concurrent calls allowed per channel.
     *
     * @return max concurrent calls
     */
    public int getMaxConcurrentCalls() {
        return maxConcurrentCalls;
    }
}
