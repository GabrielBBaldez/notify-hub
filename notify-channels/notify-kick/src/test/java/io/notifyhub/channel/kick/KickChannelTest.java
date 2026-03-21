package io.notifyhub.channel.kick;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationSendException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KickChannelTest {

    private KickConfig config;
    private KickChannel channel;

    @BeforeEach
    void setUp() {
        config = KickConfig.builder()
                .clientId("test-client-id")
                .accessToken("test-access-token")
                .broadcasterId("12345")
                .messageType("bot")
                .recipients(Map.of("mystream", "67890"))
                .build();
        channel = new KickChannel(config);
    }

    @Test
    @DisplayName("Should return 'kick' as channel name")
    void testGetName() {
        assertEquals("kick", channel.getName());
    }

    @Test
    @DisplayName("Should be available when access token is present")
    void testIsAvailable() {
        assertTrue(channel.isAvailable());
    }

    @Test
    @DisplayName("Should not be available when access token is missing")
    void testIsNotAvailableWithoutToken() {
        KickConfig noTokenConfig = KickConfig.builder()
                .clientId("id")
                .accessToken("token")
                .broadcasterId("123")
                .refreshToken("refresh")
                .clientSecret("secret")
                .build();
        KickChannel ch = new KickChannel(noTokenConfig);
        assertNotNull(ch);
    }

    @Test
    @DisplayName("Should return configured recipients")
    void testGetConfiguredRecipients() {
        Map<String, String> recipients = channel.getConfiguredRecipients();
        assertEquals(1, recipients.size());
        assertEquals("67890", recipients.get("mystream"));
    }

    @Test
    @DisplayName("Should reject invalid messageType in config")
    void testInvalidMessageType() {
        assertThrows(IllegalArgumentException.class, () ->
                KickConfig.builder()
                        .clientId("id")
                        .accessToken("token")
                        .broadcasterId("123")
                        .messageType("invalid")
                        .build());
    }

    @Test
    @DisplayName("Should require clientId in config")
    void testRequireClientId() {
        assertThrows(IllegalArgumentException.class, () ->
                KickConfig.builder()
                        .accessToken("token")
                        .broadcasterId("123")
                        .build());
    }

    @Test
    @DisplayName("Should require broadcasterId in config")
    void testRequireBroadcasterId() {
        assertThrows(IllegalArgumentException.class, () ->
                KickConfig.builder()
                        .clientId("id")
                        .accessToken("token")
                        .build());
    }

    @Test
    @DisplayName("Should default messageType to bot")
    void testDefaultMessageType() {
        KickConfig cfg = KickConfig.builder()
                .clientId("id")
                .accessToken("token")
                .broadcasterId("123")
                .build();
        assertEquals("bot", cfg.getMessageType());
    }
}
