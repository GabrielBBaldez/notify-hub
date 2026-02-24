package io.notifyhub.core.ratelimit;

/**
 * Rate limiter interface for controlling notification throughput per channel.
 *
 * <pre>{@code
 * RateLimiter limiter = new TokenBucketRateLimiter(
 *     RateLimitConfig.perMinute(10) // default: 10/min for all channels
 * );
 *
 * NotifyHub hub = NotifyHub.builder()
 *     .rateLimiter(limiter)
 *     .build();
 * }</pre>
 */
public interface RateLimiter {

    /**
     * Try to acquire a permit for the given channel.
     *
     * @param channelName the channel name (e.g., "email", "sms")
     * @return true if a permit was acquired, false if rate limit exceeded
     */
    boolean tryAcquire(String channelName);

    /**
     * Get the number of available permits for the given channel.
     *
     * @param channelName the channel name
     * @return available permits
     */
    long getAvailablePermits(String channelName);
}
