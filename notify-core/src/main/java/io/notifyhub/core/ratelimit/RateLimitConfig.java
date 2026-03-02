package io.notifyhub.core.ratelimit;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rate limit configuration: max requests within a time window.
 *
 * <pre>{@code
 * RateLimitConfig config = RateLimitConfig.perMinute(50);
 * RateLimitConfig config = RateLimitConfig.perSecond(10);
 * RateLimitConfig config = new RateLimitConfig(100, Duration.ofHours(1));
 *
 * // Use API-aware presets:
 * RateLimitConfig ytConfig = RateLimitConfig.youtube();   // 180/day
 * RateLimitConfig twConfig = RateLimitConfig.twitch();    // 20/30s
 *
 * // Get all known presets at once:
 * Map<String, RateLimitConfig> defaults = RateLimitConfig.allDefaults();
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

    // ===================== CHANNEL PRESETS =====================

    /**
     * YouTube Data API — ~200 liveChatMessages per day (10,000 quota units,
     * each message costs ~50 units). Conservative: 180/day.
     */
    public static RateLimitConfig youtube() {
        return new RateLimitConfig(180, Duration.ofDays(1));
    }

    /** Twitch Helix chat — 20 messages per 30 seconds (moderator rate). */
    public static RateLimitConfig twitch() {
        return new RateLimitConfig(20, Duration.ofSeconds(30));
    }

    /** Twitter/X API v2 — 300 tweets per 3 hours. */
    public static RateLimitConfig twitter() {
        return new RateLimitConfig(300, Duration.ofHours(3));
    }

    /** Slack webhook — 1 message per second per webhook. */
    public static RateLimitConfig slack() {
        return new RateLimitConfig(1, Duration.ofSeconds(1));
    }

    /** Telegram Bot API — 30 messages per second. */
    public static RateLimitConfig telegram() {
        return new RateLimitConfig(30, Duration.ofSeconds(1));
    }

    /** Discord webhook — 30 requests per 60 seconds. */
    public static RateLimitConfig discord() {
        return new RateLimitConfig(30, Duration.ofMinutes(1));
    }

    /** LinkedIn API — 100 posts per day. */
    public static RateLimitConfig linkedin() {
        return new RateLimitConfig(100, Duration.ofDays(1));
    }

    /** Instagram Graph API — 200 requests per hour. */
    public static RateLimitConfig instagram() {
        return new RateLimitConfig(200, Duration.ofHours(1));
    }

    /** Google Chat webhook — 60 messages per minute. */
    public static RateLimitConfig googleChat() {
        return new RateLimitConfig(60, Duration.ofMinutes(1));
    }

    /**
     * Returns the default rate limit config for a well-known channel, or null if unknown.
     *
     * @param channelName lowercase channel name (e.g., "youtube", "twitch")
     * @return preset config or null
     */
    public static RateLimitConfig forChannel(String channelName) {
        if (channelName == null) return null;
        return switch (channelName) {
            case "youtube" -> youtube();
            case "twitch" -> twitch();
            case "twitter" -> twitter();
            case "slack" -> slack();
            case "telegram" -> telegram();
            case "discord" -> discord();
            case "linkedin" -> linkedin();
            case "instagram" -> instagram();
            case "google_chat" -> googleChat();
            default -> null;
        };
    }

    /**
     * Returns a map of all known channel presets.
     * Useful for auto-configuring rate limits for all registered channels.
     */
    public static Map<String, RateLimitConfig> allDefaults() {
        Map<String, RateLimitConfig> defaults = new LinkedHashMap<>();
        defaults.put("youtube", youtube());
        defaults.put("twitch", twitch());
        defaults.put("twitter", twitter());
        defaults.put("slack", slack());
        defaults.put("telegram", telegram());
        defaults.put("discord", discord());
        defaults.put("linkedin", linkedin());
        defaults.put("instagram", instagram());
        defaults.put("google_chat", googleChat());
        return defaults;
    }

    public int getMaxRequests() { return maxRequests; }
    public Duration getWindow() { return window; }

    @Override
    public String toString() {
        return maxRequests + " per " + window;
    }
}
