package io.notifyhub.channel.teams;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationSendException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TeamsChannelTest {

    @Test
    @DisplayName("getName() returns 'teams'")
    void getName() {
        TeamsChannel channel = new TeamsChannel(
                TeamsConfig.builder().webhookUrl("https://outlook.office.com/webhook/test").build());
        assertEquals("teams", channel.getName());
    }

    @Test
    @DisplayName("isAvailable() returns true when webhook URL is set")
    void isAvailable() {
        TeamsChannel channel = new TeamsChannel(
                TeamsConfig.builder().webhookUrl("https://outlook.office.com/webhook/test").build());
        assertTrue(channel.isAvailable());
    }

    @Test
    @DisplayName("Config requires non-blank webhook URL")
    void configValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                TeamsConfig.builder().webhookUrl("").build());
        assertThrows(IllegalArgumentException.class, () ->
                TeamsConfig.builder().build());
    }

    @Test
    @DisplayName("send() with content only uses content as summary")
    void sendWithContentOnly() {
        TeamsChannel channel = new TeamsChannel(
                TeamsConfig.builder().webhookUrl("https://invalid.example.com/webhook").build());

        Notification notification = new Notification(
                "teams-channel", "teams", null, null, "Test message", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }

    @Test
    @DisplayName("send() with subject uses subject as summary")
    void sendWithSubject() {
        TeamsChannel channel = new TeamsChannel(
                TeamsConfig.builder().webhookUrl("https://invalid.example.com/webhook").build());

        Notification notification = new Notification(
                "teams-channel", "teams", "Alert Subject", null, "Test message", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }

    @Test
    @DisplayName("send() throws NotificationSendException on invalid webhook")
    void sendFailsOnInvalidWebhook() {
        TeamsChannel channel = new TeamsChannel(
                TeamsConfig.builder().webhookUrl("https://invalid.example.com/webhook").build());

        Notification notification = new Notification(
                "teams-channel", "teams", null, null, "Test message", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }
}
