package io.notifyhub.channel.linkedin;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationSendException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LinkedInChannelTest {

    private static final String ACCESS_TOKEN = "test-access-token";
    private static final String AUTHOR_ID = "urn:li:person:abc123";

    private LinkedInConfig validConfig() {
        return LinkedInConfig.builder()
                .accessToken(ACCESS_TOKEN)
                .authorId(AUTHOR_ID)
                .build();
    }

    @Test
    @DisplayName("getName() returns 'linkedin'")
    void getName() {
        LinkedInChannel channel = new LinkedInChannel(validConfig());
        assertEquals("linkedin", channel.getName());
    }

    @Test
    @DisplayName("isAvailable() returns true when access token is set")
    void isAvailable() {
        LinkedInChannel channel = new LinkedInChannel(validConfig());
        assertTrue(channel.isAvailable());
    }

    @Test
    @DisplayName("Config requires non-blank access token")
    void configRequiresAccessToken() {
        assertThrows(IllegalArgumentException.class, () ->
                LinkedInConfig.builder()
                        .accessToken("")
                        .authorId(AUTHOR_ID)
                        .build());
    }

    @Test
    @DisplayName("Config requires non-blank author ID")
    void configRequiresAuthorId() {
        assertThrows(IllegalArgumentException.class, () ->
                LinkedInConfig.builder()
                        .accessToken(ACCESS_TOKEN)
                        .authorId("")
                        .build());
    }

    @Test
    @DisplayName("send() throws NotificationSendException on network error")
    void sendFailsOnNetworkError() {
        LinkedInChannel channel = new LinkedInChannel(
                LinkedInConfig.builder()
                        .accessToken(ACCESS_TOKEN)
                        .authorId(AUTHOR_ID)
                        .timeoutMs(1000)
                        .build());

        Notification notification = new Notification(
                null, "linkedin", null, null, "Test post", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }
}
