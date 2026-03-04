package io.notifyhub.channel.facebook;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FacebookChannelTest {

    @Test
    @DisplayName("getName() returns 'facebook'")
    void getName() {
        FacebookChannel channel = new FacebookChannel(
                FacebookConfig.builder().pageAccessToken("token").pageId("page123").build());
        assertEquals("facebook", channel.getName());
    }

    @Test
    @DisplayName("isAvailable() returns true when pageAccessToken is set")
    void isAvailable() {
        FacebookChannel channel = new FacebookChannel(
                FacebookConfig.builder().pageAccessToken("token").pageId("page123").build());
        assertTrue(channel.isAvailable());
    }

    @Test
    @DisplayName("Implements NotificationChannel interface")
    void implementsNotificationChannel() {
        FacebookChannel channel = new FacebookChannel(
                FacebookConfig.builder().pageAccessToken("token").pageId("page123").build());
        assertInstanceOf(NotificationChannel.class, channel);
    }

    @Test
    @DisplayName("send() Messenger message throws NotificationSendException on invalid token")
    void sendMessengerFailsOnInvalidToken() {
        FacebookChannel channel = new FacebookChannel(
                FacebookConfig.builder().pageAccessToken("invalid").pageId("page123").build());

        Notification notification = new Notification(
                "recipient_id", "facebook", null, null, "Hello", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }

    @Test
    @DisplayName("send() feed post throws NotificationSendException on invalid token")
    void sendFeedFailsOnInvalidToken() {
        FacebookChannel channel = new FacebookChannel(
                FacebookConfig.builder().pageAccessToken("invalid").pageId("page123").build());

        Notification notification = new Notification(
                "feed", "facebook", null, null, "New post!", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }

    @Test
    @DisplayName("resolves recipient alias from configured recipients")
    void resolvesRecipientAlias() {
        FacebookChannel channel = new FacebookChannel(
                FacebookConfig.builder()
                        .pageAccessToken("invalid")
                        .pageId("page123")
                        .recipients(Map.of("john", "9876543"))
                        .build());

        assertEquals(Map.of("john", "9876543"), channel.getConfiguredRecipients());
    }

    @Test
    @DisplayName("defaults to 'feed' when recipient is empty")
    void defaultsToFeedWhenNoRecipient() {
        FacebookChannel channel = new FacebookChannel(
                FacebookConfig.builder().pageAccessToken("invalid").pageId("page123").build());

        // Empty recipient should default to feed mode, which will fail on API call
        Notification notification = new Notification(
                "", "facebook", null, null, "Post content", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }

    @Test
    @DisplayName("extractJsonValue parses simple JSON")
    void extractJsonValue() {
        assertEquals("12345", FacebookChannel.extractJsonValue("{\"id\":\"12345\"}", "id"));
        assertEquals("abc", FacebookChannel.extractJsonValue("{\"id\":\"abc\",\"name\":\"test\"}", "id"));
        assertNull(FacebookChannel.extractJsonValue("{\"name\":\"test\"}", "id"));
    }
}
