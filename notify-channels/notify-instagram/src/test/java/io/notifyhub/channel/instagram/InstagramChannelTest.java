package io.notifyhub.channel.instagram;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationSendException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InstagramChannelTest {

    @Test
    @DisplayName("getName() returns 'instagram'")
    void getName() {
        InstagramChannel channel = new InstagramChannel(
                InstagramConfig.builder().accessToken("token").igUserId("123").build());
        assertEquals("instagram", channel.getName());
    }

    @Test
    @DisplayName("isAvailable() returns true when access token is set")
    void isAvailable() {
        InstagramChannel channel = new InstagramChannel(
                InstagramConfig.builder().accessToken("token").igUserId("123").build());
        assertTrue(channel.isAvailable());
    }

    @Test
    @DisplayName("Config requires non-blank access token")
    void configValidationAccessToken() {
        assertThrows(IllegalArgumentException.class, () ->
                InstagramConfig.builder().accessToken("").igUserId("123").build());
        assertThrows(IllegalArgumentException.class, () ->
                InstagramConfig.builder().igUserId("123").build());
    }

    @Test
    @DisplayName("Config requires non-blank ig-user-id")
    void configValidationIgUserId() {
        assertThrows(IllegalArgumentException.class, () ->
                InstagramConfig.builder().accessToken("token").igUserId("").build());
        assertThrows(IllegalArgumentException.class, () ->
                InstagramConfig.builder().accessToken("token").build());
    }

    @Test
    @DisplayName("send() DM throws NotificationSendException on invalid token")
    void sendDmFailsOnInvalidToken() {
        InstagramChannel channel = new InstagramChannel(
                InstagramConfig.builder().accessToken("invalid").igUserId("123").build());

        Notification notification = new Notification(
                "recipient_id", "instagram", null, null, "Hello", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }

    @Test
    @DisplayName("send() feed post throws NotificationSendException on invalid token")
    void sendFeedFailsOnInvalidToken() {
        InstagramChannel channel = new InstagramChannel(
                InstagramConfig.builder().accessToken("invalid").igUserId("123").build());

        Notification notification = new Notification(
                "feed", "instagram", null, null, "New post!", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }

    @Test
    @DisplayName("resolves recipient alias from configured recipients")
    void resolvesRecipientAlias() {
        InstagramChannel channel = new InstagramChannel(
                InstagramConfig.builder()
                        .accessToken("invalid")
                        .igUserId("123")
                        .recipients(Map.of("john", "9876543"))
                        .build());

        assertEquals(Map.of("john", "9876543"), channel.getConfiguredRecipients());
    }

    @Test
    @DisplayName("defaults to 'feed' when recipient is empty")
    void defaultsToFeedWhenNoRecipient() {
        InstagramChannel channel = new InstagramChannel(
                InstagramConfig.builder().accessToken("invalid").igUserId("123").build());

        // Empty recipient should default to feed mode, which will fail on API call
        Notification notification = new Notification(
                "", "instagram", null, null, "Post content", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }

    @Test
    @DisplayName("extractJsonValue parses simple JSON")
    void extractJsonValue() {
        assertEquals("12345", InstagramChannel.extractJsonValue("{\"id\":\"12345\"}", "id"));
        assertEquals("abc", InstagramChannel.extractJsonValue("{\"id\":\"abc\",\"name\":\"test\"}", "id"));
        assertNull(InstagramChannel.extractJsonValue("{\"name\":\"test\"}", "id"));
    }
}
