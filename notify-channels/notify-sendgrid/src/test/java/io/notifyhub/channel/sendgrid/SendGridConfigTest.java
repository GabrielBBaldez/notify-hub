package io.notifyhub.channel.sendgrid;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SendGridConfigTest {

    @Test
    @DisplayName("Should build config with all fields")
    void testFullConfig() {
        SendGridConfig config = SendGridConfig.builder()
                .apiKey("SG.test-key")
                .from("test@example.com")
                .fromName("Test App")
                .trackOpens(true)
                .trackClicks(false)
                .timeoutMs(5000)
                .build();

        assertEquals("SG.test-key", config.getApiKey());
        assertEquals("test@example.com", config.getFrom());
        assertEquals("Test App", config.getFromName());
        assertTrue(config.isTrackOpens());
        assertFalse(config.isTrackClicks());
        assertEquals(5000, config.getTimeoutMs());
    }

    @Test
    @DisplayName("Should use default values")
    void testDefaults() {
        SendGridConfig config = SendGridConfig.builder()
                .apiKey("SG.key")
                .from("test@example.com")
                .build();

        assertTrue(config.isTrackOpens());
        assertTrue(config.isTrackClicks());
        assertEquals(10_000, config.getTimeoutMs());
        assertNull(config.getFromName());
    }

    @Test
    @DisplayName("Should throw when API key is blank")
    void testBlankApiKey() {
        assertThrows(IllegalArgumentException.class, () ->
                SendGridConfig.builder()
                        .apiKey("")
                        .from("test@example.com")
                        .build());
    }

    @Test
    @DisplayName("Should throw when API key is null")
    void testNullApiKey() {
        assertThrows(IllegalArgumentException.class, () ->
                SendGridConfig.builder()
                        .from("test@example.com")
                        .build());
    }

    @Test
    @DisplayName("Should throw when from address is blank")
    void testBlankFrom() {
        assertThrows(IllegalArgumentException.class, () ->
                SendGridConfig.builder()
                        .apiKey("SG.key")
                        .from("")
                        .build());
    }

    @Test
    @DisplayName("Should throw when from address is null")
    void testNullFrom() {
        assertThrows(IllegalArgumentException.class, () ->
                SendGridConfig.builder()
                        .apiKey("SG.key")
                        .build());
    }
}
