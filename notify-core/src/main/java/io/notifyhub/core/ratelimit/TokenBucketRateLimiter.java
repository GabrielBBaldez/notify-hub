package io.notifyhub.core.ratelimit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Token bucket rate limiter implementation.
 *
 * <p>Each channel has its own token bucket. Tokens are refilled
 * at the configured rate. A request consumes one token.</p>
 *
 * <pre>{@code
 * // Global default: 100 requests per minute
 * TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(
 *     RateLimitConfig.perMinute(100)
 * );
 *
 * // With per-channel overrides
 * TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(
 *     RateLimitConfig.perMinute(100),
 *     Map.of("email", RateLimitConfig.perMinute(50),
 *            "sms", RateLimitConfig.perMinute(10))
 * );
 * }</pre>
 */
public class TokenBucketRateLimiter implements RateLimiter {

    private final RateLimitConfig defaultConfig;
    private final Map<String, RateLimitConfig> channelConfigs;
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(RateLimitConfig defaultConfig) {
        this(defaultConfig, Map.of());
    }

    public TokenBucketRateLimiter(RateLimitConfig defaultConfig,
                                   Map<String, RateLimitConfig> channelConfigs) {
        this.defaultConfig = defaultConfig;
        this.channelConfigs = new ConcurrentHashMap<>(channelConfigs);
    }

    @Override
    public boolean tryAcquire(String channelName) {
        TokenBucket bucket = buckets.computeIfAbsent(channelName, this::createBucket);
        return bucket.tryConsume();
    }

    @Override
    public long getAvailablePermits(String channelName) {
        TokenBucket bucket = buckets.get(channelName);
        if (bucket == null) {
            RateLimitConfig config = getConfigForChannel(channelName);
            return config.getMaxRequests();
        }
        bucket.refill();
        return bucket.tokens.get();
    }

    private TokenBucket createBucket(String channelName) {
        RateLimitConfig config = getConfigForChannel(channelName);
        return new TokenBucket(config);
    }

    private RateLimitConfig getConfigForChannel(String channelName) {
        return channelConfigs.getOrDefault(channelName, defaultConfig);
    }

    private static class TokenBucket {
        private final RateLimitConfig config;
        private final AtomicLong tokens;
        private volatile long lastRefillNanos;

        TokenBucket(RateLimitConfig config) {
            this.config = config;
            this.tokens = new AtomicLong(config.getMaxRequests());
            this.lastRefillNanos = System.nanoTime();
        }

        boolean tryConsume() {
            refill();
            long current = tokens.get();
            while (current > 0) {
                if (tokens.compareAndSet(current, current - 1)) {
                    return true;
                }
                current = tokens.get();
            }
            return false;
        }

        synchronized void refill() {
            long now = System.nanoTime();
            long elapsed = now - lastRefillNanos;
            long windowNanos = config.getWindow().toNanos();

            if (elapsed >= windowNanos) {
                // Full refill
                tokens.set(config.getMaxRequests());
                lastRefillNanos = now;
            } else {
                // Partial refill
                long tokensToAdd = (elapsed * config.getMaxRequests()) / windowNanos;
                if (tokensToAdd > 0) {
                    long current = tokens.get();
                    long newTokens = Math.min(current + tokensToAdd, config.getMaxRequests());
                    tokens.compareAndSet(current, newTokens);
                    lastRefillNanos = now;
                }
            }
        }
    }
}
