package io.notifyhub.core.ratelimit;

import java.time.Duration;

/**
 * Rate limit configuration: max requests within a time window.
 *
 * <pre>{@code
 * RateLimitConfig config = RateLimitConfig.perMinute(50);
 * RateLimitConfig config = RateLimitConfig.perSecond(10);
 * RateLimitConfig config = new RateLimitConfig(100, Duration.ofHours(1));
 * }</pre>
 */
public final class RateLimitConfig {

    private final int maxRequests;
    private final Duration window;

    public RateLimitConfig(int maxRequests, Duration window) {
        if (maxRequests <= 0) throw new IllegalArgumentException("maxRequests must be positive");
        if (window == null || window.isZero() || window.isNegative())
            throw new IllegalArgumentException("window must be a positive duration");
        this.maxRequests = maxRequests;
        this.window = window;
    }

    public static RateLimitConfig perSecond(int max) {
        return new RateLimitConfig(max, Duration.ofSeconds(1));
    }

    public static RateLimitConfig perMinute(int max) {
        return new RateLimitConfig(max, Duration.ofMinutes(1));
    }

    public static RateLimitConfig perHour(int max) {
        return new RateLimitConfig(max, Duration.ofHours(1));
    }

    public int getMaxRequests() { return maxRequests; }
    public Duration getWindow() { return window; }

    @Override
    public String toString() {
        return maxRequests + " per " + window;
    }
}
