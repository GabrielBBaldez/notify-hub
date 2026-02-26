package io.notifyhub.queue.kafka;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QueuedNotificationTest {

    @Test
    @DisplayName("Builder creates notification with all fields")
    void builderCreatesNotification() {
        QueuedNotification n = QueuedNotification.builder()
                .recipient("user@test.com")
                .channelName("email")
                .subject("Welcome")
                .rawContent("Hello!")
                .priority("HIGH")
                .deduplicationKey("dedup-123")
                .params(Map.of("key", "value"))
                .build();

        assertEquals("user@test.com", n.getRecipient());
        assertEquals("email", n.getChannelName());
        assertEquals("Welcome", n.getSubject());
        assertEquals("Hello!", n.getRawContent());
        assertEquals("HIGH", n.getPriority());
        assertEquals("dedup-123", n.getDeduplicationKey());
        assertEquals("value", n.getParams().get("key"));
        assertNotNull(n.getCreatedAt());
    }

    @Test
    @DisplayName("Builder creates notification with template")
    void builderWithTemplate() {
        QueuedNotification n = QueuedNotification.builder()
                .recipient("user@test.com")
                .channelName("email")
                .templateName("welcome")
                .params(Map.of("name", "Gabriel"))
                .build();

        assertEquals("welcome", n.getTemplateName());
        assertNull(n.getRawContent());
    }

    @Test
    @DisplayName("Builder fails without recipient")
    void builderFailsWithoutRecipient() {
        assertThrows(IllegalArgumentException.class, () ->
                QueuedNotification.builder()
                        .channelName("email")
                        .rawContent("Hello")
                        .build());
    }

    @Test
    @DisplayName("Builder fails without channel")
    void builderFailsWithoutChannel() {
        assertThrows(IllegalArgumentException.class, () ->
                QueuedNotification.builder()
                        .recipient("user@test.com")
                        .rawContent("Hello")
                        .build());
    }

    @Test
    @DisplayName("Builder fails without content or template")
    void builderFailsWithoutContent() {
        assertThrows(IllegalArgumentException.class, () ->
                QueuedNotification.builder()
                        .recipient("user@test.com")
                        .channelName("email")
                        .build());
    }

    @Test
    @DisplayName("toString contains channel and recipient")
    void toStringContainsInfo() {
        QueuedNotification n = QueuedNotification.builder()
                .recipient("user@test.com")
                .channelName("slack")
                .rawContent("msg")
                .build();

        assertTrue(n.toString().contains("slack"));
        assertTrue(n.toString().contains("user@test.com"));
    }
}
