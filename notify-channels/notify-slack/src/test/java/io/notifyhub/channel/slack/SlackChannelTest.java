package io.notifyhub.channel.slack;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationSendException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SlackChannelTest {

    @Test
    @DisplayName("getName() returns 'slack'")
    void getName() {
        SlackChannel channel = new SlackChannel(
                SlackConfig.builder().webhookUrl("https://hooks.slack.com/test").build());
        assertEquals("slack", channel.getName());
    }

    @Test
    @DisplayName("isAvailable() returns true when webhook URL is set")
    void isAvailable() {
        SlackChannel channel = new SlackChannel(
                SlackConfig.builder().webhookUrl("https://hooks.slack.com/test").build());
        assertTrue(channel.isAvailable());
    }

    @Test
    @DisplayName("Config requires non-blank webhook URL")
    void configValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                SlackConfig.builder().webhookUrl("").build());
        assertThrows(IllegalArgumentException.class, () ->
                SlackConfig.builder().build());
    }

    @Test
    @DisplayName("send() throws NotificationSendException on invalid webhook")
    void sendFailsOnInvalidWebhook() {
        SlackChannel channel = new SlackChannel(
                SlackConfig.builder().webhookUrl("https://invalid.example.com/webhook").build());

        Notification notification = new Notification(
                "#general", "slack", null, null, "Test message", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }
}
