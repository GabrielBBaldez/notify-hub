package io.notifyhub.channel.notion;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationSendException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotionChannelTest {

    private static final String API_KEY = "secret_test_key";
    private static final String DATABASE_ID = "abc123def456";

    private NotionConfig validConfig() {
        return NotionConfig.builder()
                .apiKey(API_KEY)
                .databaseId(DATABASE_ID)
                .build();
    }

    @Test
    @DisplayName("getName() returns 'notion'")
    void getName() {
        NotionChannel channel = new NotionChannel(validConfig());
        assertEquals("notion", channel.getName());
    }

    @Test
    @DisplayName("isAvailable() returns true when API key is set")
    void isAvailable() {
        NotionChannel channel = new NotionChannel(validConfig());
        assertTrue(channel.isAvailable());
    }

    @Test
    @DisplayName("Config requires non-blank API key")
    void configRequiresApiKey() {
        assertThrows(IllegalArgumentException.class, () ->
                NotionConfig.builder()
                        .apiKey("")
                        .databaseId(DATABASE_ID)
                        .build());
    }

    @Test
    @DisplayName("Config requires non-blank database ID")
    void configRequiresDatabaseId() {
        assertThrows(IllegalArgumentException.class, () ->
                NotionConfig.builder()
                        .apiKey(API_KEY)
                        .databaseId("")
                        .build());
    }

    @Test
    @DisplayName("send() throws NotificationSendException on network error")
    void sendFailsOnNetworkError() {
        NotionChannel channel = new NotionChannel(
                NotionConfig.builder()
                        .apiKey(API_KEY)
                        .databaseId(DATABASE_ID)
                        .timeoutMs(1000)
                        .build());

        Notification notification = new Notification(
                null, "notion", null, null, "Test page content", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }
}
