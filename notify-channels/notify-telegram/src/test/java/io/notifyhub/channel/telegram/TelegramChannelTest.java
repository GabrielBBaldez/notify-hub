package io.notifyhub.channel.telegram;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationSendException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TelegramChannelTest {

    @Test
    @DisplayName("getName() returns 'telegram'")
    void getName() {
        TelegramChannel channel = new TelegramChannel(
                TelegramConfig.builder().botToken("123:ABC").build());
        assertEquals("telegram", channel.getName());
    }

    @Test
    @DisplayName("isAvailable() returns true when bot token is set")
    void isAvailable() {
        TelegramChannel channel = new TelegramChannel(
                TelegramConfig.builder().botToken("123:ABC").build());
        assertTrue(channel.isAvailable());
    }

    @Test
    @DisplayName("Config requires non-blank bot token")
    void configValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                TelegramConfig.builder().botToken("").build());
        assertThrows(IllegalArgumentException.class, () ->
                TelegramConfig.builder().build());
    }

    @Test
    @DisplayName("send() throws when no chat ID and no default")
    void sendFailsWithoutChatId() {
        TelegramChannel channel = new TelegramChannel(
                TelegramConfig.builder().botToken("123:ABC").build());

        Notification notification = new Notification(
                "", "telegram", null, null, "Test message", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }

    @Test
    @DisplayName("send() throws NotificationSendException on invalid bot token")
    void sendFailsOnInvalidToken() {
        TelegramChannel channel = new TelegramChannel(
                TelegramConfig.builder().botToken("invalid-token").defaultChatId("12345").build());

        Notification notification = new Notification(
                "12345", "telegram", null, null, "Test message", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }
}
