package io.notifyhub.channel.template;

import io.notifyhub.core.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TemplateChannelTest {

    private TemplateChannel channel;

    @BeforeEach
    void setUp() {
        TemplateChannelConfig config = TemplateChannelConfig.builder()
                .apiKey("test-api-key")
                .build();
        channel = new TemplateChannel(config);
    }

    @Test
    @DisplayName("Should return correct channel name")
    void getName() {
        assertEquals("template", channel.getName());
    }

    @Test
    @DisplayName("Should be available when config is valid")
    void isAvailable() {
        assertTrue(channel.isAvailable());
    }

    @Test
    @DisplayName("Should send notification without error")
    void sendNotification() {
        Notification notification = new Notification(
                "recipient@test.com", "template", "Test Subject",
                null, "Hello World", null);
        assertDoesNotThrow(() -> channel.send(notification));
    }

    @Test
    @DisplayName("Config builder should reject blank apiKey")
    void configRejectsBlankApiKey() {
        assertThrows(IllegalArgumentException.class, () ->
                TemplateChannelConfig.builder().apiKey("").build());
    }
}
