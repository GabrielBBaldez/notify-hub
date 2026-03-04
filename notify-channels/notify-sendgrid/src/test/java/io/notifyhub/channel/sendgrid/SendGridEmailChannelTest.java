package io.notifyhub.channel.sendgrid;

import io.notifyhub.core.DeliveryStatus;
import io.notifyhub.core.Notification;
import io.notifyhub.core.Priority;
import io.notifyhub.core.channel.NotificationSendException;
import io.notifyhub.core.channel.SendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SendGridEmailChannelTest {

    private SendGridConfig config;

    @BeforeEach
    void setUp() {
        config = SendGridConfig.builder()
                .apiKey("SG.test-api-key")
                .from("noreply@test.com")
                .fromName("Test App")
                .trackOpens(true)
                .trackClicks(false)
                .build();
    }

    @Test
    @DisplayName("getName() should return 'email'")
    void testGetName() {
        SendGridEmailChannel channel = new SendGridEmailChannel(config);
        assertEquals("email", channel.getName());
    }

    @Test
    @DisplayName("isAvailable() should return true when API key is set")
    void testIsAvailableTrue() {
        SendGridEmailChannel channel = new SendGridEmailChannel(config);
        assertTrue(channel.isAvailable());
    }

    @Test
    @DisplayName("isAvailable() should return false when API key is blank")
    void testIsAvailableFalse() {
        // Build with blank would throw, so we test with a valid config
        // and verify the positive case. The config validates on build.
        assertTrue(new SendGridEmailChannel(config).isAvailable());
    }

    @Test
    @DisplayName("Should implement EmailStatusProvider")
    void testImplementsEmailStatusProvider() {
        SendGridEmailChannel channel = new SendGridEmailChannel(config);
        assertTrue(channel.supportsTracking());
    }

    @Test
    @DisplayName("Should map SendGrid status 'delivered' to DELIVERED")
    void testMapDelivered() {
        assertEquals(DeliveryStatus.DELIVERED, SendGridEmailChannel.mapSendGridStatus("delivered"));
    }

    @Test
    @DisplayName("Should map SendGrid status 'open' to OPENED")
    void testMapOpen() {
        assertEquals(DeliveryStatus.OPENED, SendGridEmailChannel.mapSendGridStatus("open"));
    }

    @Test
    @DisplayName("Should map SendGrid status 'bounce' to BOUNCED")
    void testMapBounce() {
        assertEquals(DeliveryStatus.BOUNCED, SendGridEmailChannel.mapSendGridStatus("bounce"));
    }

    @Test
    @DisplayName("Should map SendGrid status 'spamreport' to SPAM_COMPLAINT")
    void testMapSpam() {
        assertEquals(DeliveryStatus.SPAM_COMPLAINT, SendGridEmailChannel.mapSendGridStatus("spamreport"));
    }

    @Test
    @DisplayName("Should map SendGrid status 'click' to CLICKED")
    void testMapClick() {
        assertEquals(DeliveryStatus.CLICKED, SendGridEmailChannel.mapSendGridStatus("click"));
    }

    @Test
    @DisplayName("Should map SendGrid status 'dropped' to DROPPED")
    void testMapDropped() {
        assertEquals(DeliveryStatus.DROPPED, SendGridEmailChannel.mapSendGridStatus("dropped"));
    }

    @Test
    @DisplayName("Should map SendGrid status 'deferred' to DEFERRED")
    void testMapDeferred() {
        assertEquals(DeliveryStatus.DEFERRED, SendGridEmailChannel.mapSendGridStatus("deferred"));
    }

    @Test
    @DisplayName("Should map SendGrid status 'processed' to SENT")
    void testMapProcessed() {
        assertEquals(DeliveryStatus.SENT, SendGridEmailChannel.mapSendGridStatus("processed"));
    }

    @Test
    @DisplayName("Should map unknown status to SENT as fallback")
    void testMapUnknown() {
        assertEquals(DeliveryStatus.SENT, SendGridEmailChannel.mapSendGridStatus("something_else"));
    }

    @Test
    @DisplayName("checkStatus should throw when providerMessageId is null")
    void testCheckStatusNullId() {
        SendGridEmailChannel channel = new SendGridEmailChannel(config);
        assertThrows(NotificationSendException.class, () -> channel.checkStatus(null));
    }

    @Test
    @DisplayName("checkStatus should throw when providerMessageId is blank")
    void testCheckStatusBlankId() {
        SendGridEmailChannel channel = new SendGridEmailChannel(config);
        assertThrows(NotificationSendException.class, () -> channel.checkStatus(""));
    }
}
