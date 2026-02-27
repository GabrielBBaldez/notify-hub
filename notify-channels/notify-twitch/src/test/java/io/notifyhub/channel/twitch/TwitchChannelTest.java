package io.notifyhub.channel.twitch;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationSendException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TwitchChannelTest {

    private static final String CLIENT_ID = "test-client-id";
    private static final String ACCESS_TOKEN = "test-access-token";
    private static final String BROADCASTER_ID = "123456789";
    private static final String SENDER_ID = "987654321";

    private TwitchConfig validConfig() {
        return TwitchConfig.builder()
                .clientId(CLIENT_ID)
                .accessToken(ACCESS_TOKEN)
                .broadcasterId(BROADCASTER_ID)
                .senderId(SENDER_ID)
                .build();
    }

    @Test
    @DisplayName("getName() returns 'twitch'")
    void getName() {
        TwitchChannel channel = new TwitchChannel(validConfig());
        assertEquals("twitch", channel.getName());
    }

    @Test
    @DisplayName("isAvailable() returns true when access token is set")
    void isAvailable() {
        TwitchChannel channel = new TwitchChannel(validConfig());
        assertTrue(channel.isAvailable());
    }

    @Test
    @DisplayName("Config requires non-blank client ID")
    void configRequiresClientId() {
        assertThrows(IllegalArgumentException.class, () ->
                TwitchConfig.builder()
                        .clientId("")
                        .accessToken(ACCESS_TOKEN)
                        .broadcasterId(BROADCASTER_ID)
                        .senderId(SENDER_ID)
                        .build());
    }

    @Test
    @DisplayName("Config requires non-blank access token")
    void configRequiresAccessToken() {
        assertThrows(IllegalArgumentException.class, () ->
                TwitchConfig.builder()
                        .clientId(CLIENT_ID)
                        .accessToken("")
                        .broadcasterId(BROADCASTER_ID)
                        .senderId(SENDER_ID)
                        .build());
    }

    @Test
    @DisplayName("Config requires non-blank broadcaster ID")
    void configRequiresBroadcasterId() {
        assertThrows(IllegalArgumentException.class, () ->
                TwitchConfig.builder()
                        .clientId(CLIENT_ID)
                        .accessToken(ACCESS_TOKEN)
                        .broadcasterId("")
                        .senderId(SENDER_ID)
                        .build());
    }

    @Test
    @DisplayName("Config requires non-blank sender ID")
    void configRequiresSenderId() {
        assertThrows(IllegalArgumentException.class, () ->
                TwitchConfig.builder()
                        .clientId(CLIENT_ID)
                        .accessToken(ACCESS_TOKEN)
                        .broadcasterId(BROADCASTER_ID)
                        .senderId("")
                        .build());
    }

    @Test
    @DisplayName("send() throws NotificationSendException on network error")
    void sendFailsOnNetworkError() {
        TwitchChannel channel = new TwitchChannel(
                TwitchConfig.builder()
                        .clientId(CLIENT_ID)
                        .accessToken(ACCESS_TOKEN)
                        .broadcasterId(BROADCASTER_ID)
                        .senderId(SENDER_ID)
                        .timeoutMs(1000)
                        .build());

        Notification notification = new Notification(
                null, "twitch", null, null, "Test message", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }
}
