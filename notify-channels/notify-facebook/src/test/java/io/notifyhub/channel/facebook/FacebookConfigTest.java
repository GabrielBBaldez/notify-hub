package io.notifyhub.channel.facebook;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FacebookConfigTest {

    @Test
    @DisplayName("Should build config with all fields")
    void shouldBuildWithAllFields() {
        Map<String, String> recipients = Map.of("john", "123456789");

        FacebookConfig config = FacebookConfig.builder()
                .pageAccessToken("EAAGm0PX4ZCps...")
                .pageId("102345678901234")
                .recipients(recipients)
                .timeoutMs(5_000)
                .build();

        assertEquals("EAAGm0PX4ZCps...", config.getPageAccessToken());
        assertEquals("102345678901234", config.getPageId());
        assertEquals(Map.of("john", "123456789"), config.getRecipients());
        assertEquals(5_000, config.getTimeoutMs());
    }

    @Test
    @DisplayName("Should use default timeoutMs of 10000")
    void shouldUseDefaults() {
        FacebookConfig config = FacebookConfig.builder()
                .pageAccessToken("token")
                .pageId("page123")
                .build();

        assertEquals(10_000, config.getTimeoutMs());
        assertTrue(config.getRecipients().isEmpty());
    }

    @Test
    @DisplayName("Should throw when pageAccessToken is blank")
    void shouldThrowWhenPageAccessTokenBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                FacebookConfig.builder()
                        .pageAccessToken("")
                        .pageId("page123")
                        .build());
    }

    @Test
    @DisplayName("Should throw when pageAccessToken is null")
    void shouldThrowWhenPageAccessTokenNull() {
        assertThrows(IllegalArgumentException.class, () ->
                FacebookConfig.builder()
                        .pageId("page123")
                        .build());
    }

    @Test
    @DisplayName("Should throw when pageId is blank")
    void shouldThrowWhenPageIdBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                FacebookConfig.builder()
                        .pageAccessToken("token")
                        .pageId("")
                        .build());
    }

    @Test
    @DisplayName("Should throw when pageId is null")
    void shouldThrowWhenPageIdNull() {
        assertThrows(IllegalArgumentException.class, () ->
                FacebookConfig.builder()
                        .pageAccessToken("token")
                        .build());
    }
}
