package io.notifyhub.channel.googlechat;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationSendException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GoogleChatChannelTest {

    @Test
    @DisplayName("getName() returns 'google-chat'")
    void getName() {
        GoogleChatChannel channel = new GoogleChatChannel(
                GoogleChatConfig.builder()
                        .webhookUrl("https://chat.googleapis.com/v1/spaces/test/messages?key=k&token=t")
                        .build());
        assertEquals("google-chat", channel.getName());
    }

    @Test
    @DisplayName("isAvailable() returns true when webhook URL is set")
    void isAvailable() {
        GoogleChatChannel channel = new GoogleChatChannel(
                GoogleChatConfig.builder()
                        .webhookUrl("https://chat.googleapis.com/v1/spaces/test/messages?key=k&token=t")
                        .build());
        assertTrue(channel.isAvailable());
    }

    @Test
    @DisplayName("Config requires non-blank webhook URL")
    void configValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                GoogleChatConfig.builder().webhookUrl("").build());
        assertThrows(IllegalArgumentException.class, () ->
                GoogleChatConfig.builder().build());
    }

    @Test
    @DisplayName("Config default timeout is 10000ms")
    void configDefaults() {
        GoogleChatConfig config = GoogleChatConfig.builder()
                .webhookUrl("https://chat.googleapis.com/v1/spaces/test/messages?key=k&token=t")
                .build();
        assertEquals(10_000, config.getTimeoutMs());
    }

    @Test
    @DisplayName("Config custom timeout is preserved")
    void configCustomTimeout() {
        GoogleChatConfig config = GoogleChatConfig.builder()
                .webhookUrl("https://chat.googleapis.com/v1/spaces/test/messages?key=k&token=t")
                .timeoutMs(5000)
                .build();
        assertEquals(5000, config.getTimeoutMs());
    }

    @Test
    @DisplayName("send() throws NotificationSendException on invalid webhook")
    void sendFailsOnInvalidWebhook() {
        GoogleChatChannel channel = new GoogleChatChannel(
                GoogleChatConfig.builder()
                        .webhookUrl("https://invalid.example.com/webhook")
                        .timeoutMs(2000)
                        .build());

        Notification notification = new Notification(
                "space-id", "google-chat", null, null, "Test message", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }
}
