package io.notifyhub.channel.tiktokshop;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationSendException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TikTokShopChannelTest {

    private static final String APP_KEY = "test-app-key";
    private static final String APP_SECRET = "test-app-secret";
    private static final String ACCESS_TOKEN = "test-access-token";

    private TikTokShopConfig validConfig() {
        return TikTokShopConfig.builder()
                .appKey(APP_KEY)
                .appSecret(APP_SECRET)
                .accessToken(ACCESS_TOKEN)
                .build();
    }

    @Test
    @DisplayName("getName() returns 'tiktok_shop'")
    void getName() {
        TikTokShopChannel channel = new TikTokShopChannel(validConfig());
        assertEquals("tiktok-shop", channel.getName());
    }

    @Test
    @DisplayName("isAvailable() returns true when appKey is set")
    void isAvailable() {
        TikTokShopChannel channel = new TikTokShopChannel(validConfig());
        assertTrue(channel.isAvailable());
    }

    @Test
    @DisplayName("isAvailable() returns true with full config")
    void isAvailableWithFullConfig() {
        TikTokShopConfig config = TikTokShopConfig.builder()
                .appKey(APP_KEY)
                .appSecret(APP_SECRET)
                .accessToken(ACCESS_TOKEN)
                .shopId("shop-123")
                .recipients(Map.of("buyer", "user456"))
                .build();

        TikTokShopChannel channel = new TikTokShopChannel(config);
        assertTrue(channel.isAvailable());
    }

    @Test
    @DisplayName("generateSign() produces consistent HMAC-SHA256 signature")
    void generateSignProducesConsistentSignature() {
        TikTokShopChannel channel = new TikTokShopChannel(validConfig());

        String sign1 = channel.generateSign("/api/push/message", 1700000000L);
        String sign2 = channel.generateSign("/api/push/message", 1700000000L);

        assertNotNull(sign1);
        assertFalse(sign1.isBlank());
        assertEquals(sign1, sign2, "Same inputs should produce the same signature");
        assertEquals(64, sign1.length(), "HMAC-SHA256 hex output should be 64 characters");
    }

    @Test
    @DisplayName("generateSign() produces different signatures for different timestamps")
    void generateSignDiffersForDifferentTimestamps() {
        TikTokShopChannel channel = new TikTokShopChannel(validConfig());

        String sign1 = channel.generateSign("/api/push/message", 1700000000L);
        String sign2 = channel.generateSign("/api/push/message", 1700000001L);

        assertNotEquals(sign1, sign2, "Different timestamps should produce different signatures");
    }

    @Test
    @DisplayName("send() throws NotificationSendException on network error")
    void sendFailsOnNetworkError() {
        TikTokShopChannel channel = new TikTokShopChannel(
                TikTokShopConfig.builder()
                        .appKey(APP_KEY)
                        .appSecret(APP_SECRET)
                        .accessToken(ACCESS_TOKEN)
                        .timeoutMs(1000)
                        .build());

        Notification notification = new Notification(
                null, "tiktok-shop", null, null, "Order shipped!", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }

    @Test
    @DisplayName("getConfiguredRecipients() returns config recipients")
    void getConfiguredRecipients() {
        Map<String, String> recipients = Map.of("buyer", "user123", "seller", "user456");
        TikTokShopConfig config = TikTokShopConfig.builder()
                .appKey(APP_KEY)
                .appSecret(APP_SECRET)
                .accessToken(ACCESS_TOKEN)
                .recipients(recipients)
                .build();

        TikTokShopChannel channel = new TikTokShopChannel(config);
        assertEquals(2, channel.getConfiguredRecipients().size());
        assertEquals("user123", channel.getConfiguredRecipients().get("buyer"));
    }
}
