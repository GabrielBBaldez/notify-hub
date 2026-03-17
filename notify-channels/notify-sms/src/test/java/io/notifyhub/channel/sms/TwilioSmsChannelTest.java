package io.notifyhub.channel.sms;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationSendException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TwilioSmsChannelTest {

    private TwilioConfig testConfig() {
        return TwilioConfig.builder()
                .accountSid("ACtest000000000000000000000000000")
                .authToken("test-auth-token")
                .fromNumber("+15551234567")
                .build();
    }

    @Test
    @DisplayName("getName() returns 'sms'")
    void getName() {
        TwilioSmsChannel channel = new TwilioSmsChannel(testConfig());
        assertEquals("sms", channel.getName());
    }

    @Test
    @DisplayName("isAvailable() returns true when config is valid")
    void isAvailable() {
        TwilioSmsChannel channel = new TwilioSmsChannel(testConfig());
        assertTrue(channel.isAvailable());
    }

    @Test
    @DisplayName("send() throws NotificationSendException with invalid credentials")
    void sendFailsWithInvalidCredentials() {
        TwilioSmsChannel channel = new TwilioSmsChannel(testConfig());
        Notification notification = new Notification(
                "+15559876543", "sms", null, null, "Test SMS message", Map.of());
        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }
}
