package io.notifyhub.channel.push.firebase;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationSendException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FirebasePushChannelTest {

    @Test
    @DisplayName("getName() returns 'push'")
    void getName() {
        FirebasePushChannel channel = new FirebasePushChannel(
                FirebasePushConfig.builder().serverKey("test-server-key").build());
        assertEquals("push", channel.getName());
    }

    @Test
    @DisplayName("isAvailable() returns true when server key is set")
    void isAvailable() {
        FirebasePushChannel channel = new FirebasePushChannel(
                FirebasePushConfig.builder().serverKey("test-server-key").build());
        assertTrue(channel.isAvailable());
    }

    @Test
    @DisplayName("Config requires non-blank server key")
    void configValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                FirebasePushConfig.builder().serverKey("").build());
        assertThrows(IllegalArgumentException.class, () ->
                FirebasePushConfig.builder().build());
    }

    @Test
    @DisplayName("Config uses default timeout of 10000ms")
    void configDefaultTimeout() {
        FirebasePushConfig config = FirebasePushConfig.builder()
                .serverKey("test-server-key")
                .build();
        assertEquals(10_000, config.getTimeoutMs());
    }

    @Test
    @DisplayName("Config allows custom timeout")
    void configCustomTimeout() {
        FirebasePushConfig config = FirebasePushConfig.builder()
                .serverKey("test-server-key")
                .timeoutMs(5_000)
                .build();
        assertEquals(5_000, config.getTimeoutMs());
    }

    @Test
    @DisplayName("send() throws when no device token (recipient) provided")
    void sendFailsWithoutRecipient() {
        FirebasePushChannel channel = new FirebasePushChannel(
                FirebasePushConfig.builder().serverKey("test-server-key").build());

        Notification notification = new Notification(
                "", "push", "Test Title", null, "Test message", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }

    @Test
    @DisplayName("send() throws when recipient is null")
    void sendFailsWithNullRecipient() {
        FirebasePushChannel channel = new FirebasePushChannel(
                FirebasePushConfig.builder().serverKey("test-server-key").build());

        Notification notification = new Notification(
                null, "push", "Test Title", null, "Test message", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }

    @Test
    @DisplayName("send() throws NotificationSendException on invalid server key")
    void sendFailsOnInvalidServerKey() {
        FirebasePushChannel channel = new FirebasePushChannel(
                FirebasePushConfig.builder().serverKey("invalid-key").build());

        Notification notification = new Notification(
                "device-token-123", "push", "Test Title", null, "Test message", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }
}
