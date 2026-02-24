package io.notifyhub.channel.discord;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationSendException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DiscordChannelTest {

    @Test
    @DisplayName("getName() returns 'discord'")
    void getName() {
        DiscordChannel channel = new DiscordChannel(
                DiscordConfig.builder().webhookUrl("https://discord.com/api/webhooks/123/abc").build());
        assertEquals("discord", channel.getName());
    }

    @Test
    @DisplayName("isAvailable() returns true when webhook URL is set")
    void isAvailable() {
        DiscordChannel channel = new DiscordChannel(
                DiscordConfig.builder().webhookUrl("https://discord.com/api/webhooks/123/abc").build());
        assertTrue(channel.isAvailable());
    }

    @Test
    @DisplayName("Config requires non-blank webhook URL")
    void configValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                DiscordConfig.builder().webhookUrl("").build());
        assertThrows(IllegalArgumentException.class, () ->
                DiscordConfig.builder().build());
    }

    @Test
    @DisplayName("send() throws NotificationSendException on invalid webhook")
    void sendFailsOnInvalidWebhook() {
        DiscordChannel channel = new DiscordChannel(
                DiscordConfig.builder().webhookUrl("https://invalid.example.com/webhook").build());

        Notification notification = new Notification(
                "#general", "discord", null, null, "Test message", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }
}
