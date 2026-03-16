package io.notifyhub.channel.mailgun;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationSendException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MailgunChannel")
class MailgunChannelTest {

    private static MailgunConfig validConfig() {
        return MailgunConfig.builder()
                .apiKey("key-abc123")
                .domain("mail.example.com")
                .from("noreply@example.com")
                .build();
    }

    @Test
    @DisplayName("getName() returns 'mailgun'")
    void getName() {
        MailgunChannel channel = new MailgunChannel(validConfig());
        assertEquals("mailgun", channel.getName());
    }

    @Test
    @DisplayName("isAvailable() returns true when apiKey and domain are set")
    void isAvailable() {
        MailgunChannel channel = new MailgunChannel(validConfig());
        assertTrue(channel.isAvailable());
    }

    @Test
    @DisplayName("Config requires non-blank apiKey")
    void configRequiresApiKey() {
        assertThrows(IllegalArgumentException.class, () ->
                MailgunConfig.builder()
                        .apiKey("")
                        .domain("mail.example.com")
                        .from("noreply@example.com")
                        .build());
    }

    @Test
    @DisplayName("Config requires non-blank domain")
    void configRequiresDomain() {
        assertThrows(IllegalArgumentException.class, () ->
                MailgunConfig.builder()
                        .apiKey("key-abc123")
                        .domain(null)
                        .from("noreply@example.com")
                        .build());
    }

    @Test
    @DisplayName("send() throws NotificationSendException on invalid endpoint")
    void sendFailsOnInvalidEndpoint() {
        // Use an invalid domain so the HTTP call fails
        MailgunConfig config = MailgunConfig.builder()
                .apiKey("key-test")
                .domain("invalid.example.invalid")
                .from("noreply@example.com")
                .timeoutMs(2_000)
                .build();

        MailgunChannel channel = new MailgunChannel(config);

        Notification notification = new Notification(
                "recipient@example.com", "mailgun", "Test Subject", null, "Hello!", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }

    @Test
    @DisplayName("send() uses subject param when provided")
    void sendUsesSubjectParam() {
        // Use an invalid domain so the HTTP call fails — we just verify no NPE on subject resolution
        MailgunConfig config = MailgunConfig.builder()
                .apiKey("key-test")
                .domain("invalid.example.invalid")
                .from("noreply@example.com")
                .timeoutMs(2_000)
                .build();

        MailgunChannel channel = new MailgunChannel(config);

        Notification notification = new Notification(
                "recipient@example.com", "mailgun", null, null, "Hello!",
                Map.of("subject", "My Custom Subject"));

        // We expect a send failure (invalid endpoint), not an NPE or IllegalArgumentException
        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }
}
