package io.notifyhub.channel.whatsapp;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WhatsAppCloudChannelTest {

    @Test
    @DisplayName("getName() returns 'whatsapp'")
    void getName() {
        WhatsAppCloudChannel channel = new WhatsAppCloudChannel(
                WhatsAppCloudConfig.builder().accessToken("token").phoneNumberId("phone123").build());
        assertEquals("whatsapp", channel.getName());
    }

    @Test
    @DisplayName("isAvailable() returns true when accessToken is set")
    void isAvailable() {
        WhatsAppCloudChannel channel = new WhatsAppCloudChannel(
                WhatsAppCloudConfig.builder().accessToken("token").phoneNumberId("phone123").build());
        assertTrue(channel.isAvailable());
    }

    @Test
    @DisplayName("Implements NotificationChannel interface")
    void implementsNotificationChannel() {
        WhatsAppCloudChannel channel = new WhatsAppCloudChannel(
                WhatsAppCloudConfig.builder().accessToken("token").phoneNumberId("phone123").build());
        assertInstanceOf(NotificationChannel.class, channel);
    }

    @Test
    @DisplayName("send() text message throws NotificationSendException on invalid token")
    void sendTextFailsOnInvalidToken() {
        WhatsAppCloudChannel channel = new WhatsAppCloudChannel(
                WhatsAppCloudConfig.builder().accessToken("invalid").phoneNumberId("phone123").build());

        Notification notification = new Notification(
                "+5548999999999", "whatsapp", null, null, "Hello!", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }

    @Test
    @DisplayName("send() with mediaUrl throws NotificationSendException on invalid token")
    void sendMediaFailsOnInvalidToken() {
        WhatsAppCloudChannel channel = new WhatsAppCloudChannel(
                WhatsAppCloudConfig.builder().accessToken("invalid").phoneNumberId("phone123").build());

        Notification notification = new Notification(
                "+5548999999999", "whatsapp", null, null, "Check this!",
                Map.of("mediaUrl", "https://example.com/image.jpg"));

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }

    @Test
    @DisplayName("resolves recipient alias from configured recipients")
    void resolvesRecipientAlias() {
        WhatsAppCloudChannel channel = new WhatsAppCloudChannel(
                WhatsAppCloudConfig.builder()
                        .accessToken("token")
                        .phoneNumberId("phone123")
                        .recipients(Map.of("john", "+5548999999999"))
                        .build());

        assertEquals(Map.of("john", "+5548999999999"), channel.getConfiguredRecipients());
    }

    @Test
    @DisplayName("normalizePhone strips whatsapp: prefix and + sign")
    void normalizePhone() {
        assertEquals("5548999999999", WhatsAppCloudChannel.normalizePhone("whatsapp:+5548999999999"));
        assertEquals("5548999999999", WhatsAppCloudChannel.normalizePhone("+5548999999999"));
        assertEquals("5548999999999", WhatsAppCloudChannel.normalizePhone("5548999999999"));
        assertEquals("", WhatsAppCloudChannel.normalizePhone(null));
    }

    @Test
    @DisplayName("detectMediaType detects image, video, and document from URL")
    void detectMediaType() {
        assertEquals("image", WhatsAppCloudChannel.detectMediaType("https://example.com/photo.jpg", Map.of()));
        assertEquals("image", WhatsAppCloudChannel.detectMediaType("https://example.com/photo.png", Map.of()));
        assertEquals("image", WhatsAppCloudChannel.detectMediaType("https://example.com/photo.webp", Map.of()));
        assertEquals("video", WhatsAppCloudChannel.detectMediaType("https://example.com/clip.mp4", Map.of()));
        assertEquals("document", WhatsAppCloudChannel.detectMediaType("https://example.com/file.pdf", Map.of()));
        assertEquals("document", WhatsAppCloudChannel.detectMediaType("https://example.com/unknown", Map.of()));
    }

    @Test
    @DisplayName("detectMediaType respects explicit mediaType param")
    void detectMediaTypeExplicit() {
        assertEquals("video", WhatsAppCloudChannel.detectMediaType(
                "https://example.com/file.jpg", Map.of("mediaType", "video")));
        assertEquals("image", WhatsAppCloudChannel.detectMediaType(
                "https://example.com/file.pdf", Map.of("mediaType", "image")));
    }

    @Test
    @DisplayName("detectMediaType strips query params for extension detection")
    void detectMediaTypeStripsQueryParams() {
        assertEquals("image", WhatsAppCloudChannel.detectMediaType(
                "https://example.com/photo.jpg?token=abc123", Map.of()));
    }

    @Test
    @DisplayName("extractMessageId parses wamid from Cloud API response")
    void extractMessageId() {
        String response = "{\"messaging_product\":\"whatsapp\",\"contacts\":[{\"input\":\"+55\",\"wa_id\":\"55\"}],\"messages\":[{\"id\":\"wamid.HBgNNTU0ODk5MTIzNDU2FQ\"}]}";
        assertEquals("wamid.HBgNNTU0ODk5MTIzNDU2FQ", WhatsAppCloudChannel.extractMessageId(response));
    }

    @Test
    @DisplayName("extractMessageId returns null for invalid response")
    void extractMessageIdInvalid() {
        assertNull(WhatsAppCloudChannel.extractMessageId("{\"error\":\"bad\"}"));
        assertNull(WhatsAppCloudChannel.extractMessageId(null));
    }
}
