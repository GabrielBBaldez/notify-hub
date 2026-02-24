package io.notifyhub.core.retry;

import java.time.Duration;

/**
 * Defines retry behavior for failed notification sending.
 *
 * <pre>{@code
 * // Exponential backoff: 1s, 2s, 4s
 * RetryPolicy.exponential(3)
 *
 * // Fixed interval: 2s, 2s, 2s
 * RetryPolicy.fixed(3, Duration.ofSeconds(2))
 *
 * // No retry
 * RetryPolicy.none()
 * }</pre>
 */
public final class RetryPolicy {

    public enum BackoffStrategy {
        NONE,
        FIXED,
        EXPONENTIAL
    }

    private final int maxAttempts;
    private final Duration initialDelay;
    private final BackoffStrategy strategy;

    private RetryPolicy(int maxAttempts, Duration initialDelay, BackoffStrategy strategy) {
        this.maxAttempts = maxAttempts;
        this.initialDelay = initialDelay;
        this.strategy = strategy;
    }

    /** No retry — fail immediately. */
    public static RetryPolicy none() {
        return new RetryPolicy(1, Duration.ZERO, BackoffStrategy.NONE);
    }

    /** Fixed delay between retries. */
    public static RetryPolicy fixed(int maxAttempts, Duration delay) {
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be >= 1");
        return new RetryPolicy(maxAttempts, delay, BackoffStrategy.FIXED);
    }

    /** Exponential backoff starting at 1 second: 1s, 2s, 4s, 8s... */
    public static RetryPolicy exponential(int maxAttempts) {
        return exponential(maxAttempts, Duration.ofSeconds(1));
    }

    /** Exponential backoff with custom initial delay. */
    public static RetryPolicy exponential(int maxAttempts, Duration initialDelay) {
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be >= 1");
        return new RetryPolicy(maxAttempts, initialDelay, BackoffStrategy.EXPONENTIAL);
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Duration getInitialDelay() {
        return initialDelay;
    }

    public BackoffStrategy getStrategy() {
        return strategy;
    }

    /**
     * Calculate delay for a given attempt number (0-indexed).
     */
    public Duration getDelayForAttempt(int attempt) {
        return switch (strategy) {
            case NONE -> Duration.ZERO;
            case FIXED -> initialDelay;
            case EXPONENTIAL -> initialDelay.multipliedBy((long) Math.pow(2, attempt));
        };
    }

    public boolean shouldRetry() {
        return maxAttempts > 1;
    }

    @Override
    public String toString() {
        return "RetryPolicy{" +
                "strategy=" + strategy +
                ", maxAttempts=" + maxAttempts +
                ", initialDelay=" + initialDelay +
                '}';
    }
}
