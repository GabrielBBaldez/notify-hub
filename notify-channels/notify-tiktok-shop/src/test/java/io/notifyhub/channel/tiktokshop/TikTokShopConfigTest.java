package io.notifyhub.channel.tiktokshop;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TikTokShopConfigTest {

    private static final String APP_KEY = "test-app-key";
    private static final String APP_SECRET = "test-app-secret";
    private static final String ACCESS_TOKEN = "test-access-token";

    @Test
    @DisplayName("Should build config with all fields")
    void shouldBuildWithAllFields() {
        Map<String, String> recipients = Map.of("buyer", "user123");

        TikTokShopConfig config = TikTokShopConfig.builder()
                .appKey(APP_KEY)
                .appSecret(APP_SECRET)
                .accessToken(ACCESS_TOKEN)
                .shopId("shop-456")
                .recipients(recipients)
                .timeoutMs(5_000)
                .build();

        assertEquals(APP_KEY, config.getAppKey());
        assertEquals(APP_SECRET, config.getAppSecret());
        assertEquals(ACCESS_TOKEN, config.getAccessToken());
        assertEquals("shop-456", config.getShopId());
        assertEquals(1, config.getRecipients().size());
        assertEquals("user123", config.getRecipients().get("buyer"));
        assertEquals(5_000, config.getTimeoutMs());
    }

    @Test
    @DisplayName("Should use default timeoutMs of 10000")
    void shouldUseDefaults() {
        TikTokShopConfig config = TikTokShopConfig.builder()
                .appKey(APP_KEY)
                .appSecret(APP_SECRET)
                .accessToken(ACCESS_TOKEN)
                .build();

        assertEquals(10_000, config.getTimeoutMs());
        assertNull(config.getShopId());
        assertTrue(config.getRecipients().isEmpty());
    }

    @Test
    @DisplayName("Should throw when appKey is blank")
    void shouldThrowWhenAppKeyBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                TikTokShopConfig.builder()
                        .appKey("")
                        .appSecret(APP_SECRET)
                        .accessToken(ACCESS_TOKEN)
                        .build());
    }

    @Test
    @DisplayName("Should throw when appKey is null")
    void shouldThrowWhenAppKeyNull() {
        assertThrows(IllegalArgumentException.class, () ->
                TikTokShopConfig.builder()
                        .appSecret(APP_SECRET)
                        .accessToken(ACCESS_TOKEN)
                        .build());
    }

    @Test
    @DisplayName("Should throw when appSecret is blank")
    void shouldThrowWhenAppSecretBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                TikTokShopConfig.builder()
                        .appKey(APP_KEY)
                        .appSecret("")
                        .accessToken(ACCESS_TOKEN)
                        .build());
    }

    @Test
    @DisplayName("Should throw when appSecret is null")
    void shouldThrowWhenAppSecretNull() {
        assertThrows(IllegalArgumentException.class, () ->
                TikTokShopConfig.builder()
                        .appKey(APP_KEY)
                        .accessToken(ACCESS_TOKEN)
                        .build());
    }

    @Test
    @DisplayName("Should throw when accessToken is blank")
    void shouldThrowWhenAccessTokenBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                TikTokShopConfig.builder()
                        .appKey(APP_KEY)
                        .appSecret(APP_SECRET)
                        .accessToken("")
                        .build());
    }

    @Test
    @DisplayName("Should throw when accessToken is null")
    void shouldThrowWhenAccessTokenNull() {
        assertThrows(IllegalArgumentException.class, () ->
                TikTokShopConfig.builder()
                        .appKey(APP_KEY)
                        .appSecret(APP_SECRET)
                        .build());
    }

    @Test
    @DisplayName("Recipients map should be unmodifiable")
    void recipientsShouldBeUnmodifiable() {
        TikTokShopConfig config = TikTokShopConfig.builder()
                .appKey(APP_KEY)
                .appSecret(APP_SECRET)
                .accessToken(ACCESS_TOKEN)
                .recipients(Map.of("buyer", "user123"))
                .build();

        assertThrows(UnsupportedOperationException.class, () ->
                config.getRecipients().put("new", "value"));
    }
}
