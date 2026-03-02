package io.notifyhub.core.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitConfigTest {

    @Test
    void youtube_returns180PerDay() {
        RateLimitConfig config = RateLimitConfig.youtube();
        assertEquals(180, config.getMaxRequests());
        assertEquals(Duration.ofDays(1), config.getWindow());
    }

    @Test
    void twitch_returns20Per30Seconds() {
        RateLimitConfig config = RateLimitConfig.twitch();
        assertEquals(20, config.getMaxRequests());
        assertEquals(Duration.ofSeconds(30), config.getWindow());
    }

    @Test
    void twitter_returns300Per3Hours() {
        RateLimitConfig config = RateLimitConfig.twitter();
        assertEquals(300, config.getMaxRequests());
        assertEquals(Duration.ofHours(3), config.getWindow());
    }

    @Test
    void slack_returns1PerSecond() {
        RateLimitConfig config = RateLimitConfig.slack();
        assertEquals(1, config.getMaxRequests());
        assertEquals(Duration.ofSeconds(1), config.getWindow());
    }

    @Test
    void telegram_returns30PerSecond() {
        RateLimitConfig config = RateLimitConfig.telegram();
        assertEquals(30, config.getMaxRequests());
        assertEquals(Duration.ofSeconds(1), config.getWindow());
    }

    @Test
    void discord_returns30PerMinute() {
        RateLimitConfig config = RateLimitConfig.discord();
        assertEquals(30, config.getMaxRequests());
        assertEquals(Duration.ofMinutes(1), config.getWindow());
    }

    @Test
    void linkedin_returns100PerDay() {
        RateLimitConfig config = RateLimitConfig.linkedin();
        assertEquals(100, config.getMaxRequests());
        assertEquals(Duration.ofDays(1), config.getWindow());
    }

    @Test
    void instagram_returns200PerHour() {
        RateLimitConfig config = RateLimitConfig.instagram();
        assertEquals(200, config.getMaxRequests());
        assertEquals(Duration.ofHours(1), config.getWindow());
    }

    @Test
    void googleChat_returns60PerMinute() {
        RateLimitConfig config = RateLimitConfig.googleChat();
        assertEquals(60, config.getMaxRequests());
        assertEquals(Duration.ofMinutes(1), config.getWindow());
    }

    @Test
    void forChannel_returnsCorrectPreset() {
        assertNotNull(RateLimitConfig.forChannel("youtube"));
        assertNotNull(RateLimitConfig.forChannel("twitch"));
        assertNotNull(RateLimitConfig.forChannel("slack"));
        assertEquals(180, RateLimitConfig.forChannel("youtube").getMaxRequests());
        assertEquals(20, RateLimitConfig.forChannel("twitch").getMaxRequests());
    }

    @Test
    void forChannel_returnsNullForUnknown() {
        assertNull(RateLimitConfig.forChannel("email"));
        assertNull(RateLimitConfig.forChannel("sms"));
        assertNull(RateLimitConfig.forChannel("unknown"));
        assertNull(RateLimitConfig.forChannel(null));
    }

    @Test
    void allDefaults_containsAllKnownChannels() {
        Map<String, RateLimitConfig> defaults = RateLimitConfig.allDefaults();
        assertEquals(9, defaults.size());
        assertTrue(defaults.containsKey("youtube"));
        assertTrue(defaults.containsKey("twitch"));
        assertTrue(defaults.containsKey("twitter"));
        assertTrue(defaults.containsKey("slack"));
        assertTrue(defaults.containsKey("telegram"));
        assertTrue(defaults.containsKey("discord"));
        assertTrue(defaults.containsKey("linkedin"));
        assertTrue(defaults.containsKey("instagram"));
        assertTrue(defaults.containsKey("google_chat"));
    }
}
