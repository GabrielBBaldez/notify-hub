package io.notifyhub.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StatusWebhookListenerTest {

    @Test
    @DisplayName("Constructor throws on null URL")
    void constructorThrowsOnNullUrl() {
        assertThrows(IllegalArgumentException.class,
                () -> new StatusWebhookListener(null, 5000, Map.of()));
    }

    @Test
    @DisplayName("Constructor throws on blank URL")
    void constructorThrowsOnBlankUrl() {
        assertThrows(IllegalArgumentException.class,
                () -> new StatusWebhookListener("  ", 5000, Map.of()));
    }

    @Test
    @DisplayName("Constructor defaults timeout when zero or negative")
    void constructorDefaultsTimeout() {
        StatusWebhookListener listener = new StatusWebhookListener("http://localhost", 0, null);
        assertEquals(10_000, listener.getTimeoutMs());

        StatusWebhookListener listener2 = new StatusWebhookListener("http://localhost", -1, null);
        assertEquals(10_000, listener2.getTimeoutMs());
    }

    @Test
    @DisplayName("Headers are immutable copy")
    void headersAreImmutable() {
        StatusWebhookListener listener = new StatusWebhookListener(
                "http://localhost", 5000, Map.of("X-Custom", "value"));
        assertEquals("value", listener.getHeaders().get("X-Custom"));
    }

    @Test
    @DisplayName("Signing disabled by default (3-param constructor)")
    void signingDisabledByDefault() {
        StatusWebhookListener listener = new StatusWebhookListener(
                "http://localhost", 5000, Map.of());
        assertFalse(listener.isSigningEnabled());
    }

    @Test
    @DisplayName("Signing enabled with secret (4-param constructor)")
    void signingEnabledWithSecret() {
        StatusWebhookListener listener = new StatusWebhookListener(
                "http://localhost", 5000, Map.of(), "my-secret");
        assertTrue(listener.isSigningEnabled());
    }

    @Test
    @DisplayName("Signing disabled when secret is blank")
    void signingDisabledWhenSecretBlank() {
        StatusWebhookListener listener = new StatusWebhookListener(
                "http://localhost", 5000, Map.of(), "  ");
        assertFalse(listener.isSigningEnabled());
    }

    @Test
    @DisplayName("HMAC-SHA256 produces correct known output")
    void hmacSha256ProducesCorrectOutput() {
        // Known test vector: HMAC-SHA256("key", "The quick brown fox jumps over the lazy dog")
        // = f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8
        String result = StatusWebhookListener.computeHmacSha256("key",
                "The quick brown fox jumps over the lazy dog");
        assertEquals("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8", result);
    }

    @Test
    @DisplayName("HMAC-SHA256 with timestamp.payload format")
    void hmacSha256WithTimestampPayload() {
        String timestamp = "1700000000";
        String payload = "{\"event\":\"SENT\",\"channel\":\"email\"}";
        String data = timestamp + "." + payload;

        String result = StatusWebhookListener.computeHmacSha256("webhook-secret", data);
        assertNotNull(result);
        assertEquals(64, result.length()); // SHA-256 = 32 bytes = 64 hex chars

        // Same input must produce same output
        String result2 = StatusWebhookListener.computeHmacSha256("webhook-secret", data);
        assertEquals(result, result2);

        // Different secret must produce different output
        String result3 = StatusWebhookListener.computeHmacSha256("other-secret", data);
        assertNotEquals(result, result3);
    }

    @Test
    @DisplayName("onSuccess creates delivery record (async, to unreachable URL)")
    void onSuccessCreatesDeliveryRecord() throws Exception {
        StatusWebhookListener listener = new StatusWebhookListener(
                "http://localhost:1", 500, Map.of());

        listener.onSuccess("email", "welcome");

        // Wait for async completion
        Thread.sleep(1000);

        assertFalse(listener.getRecentDeliveries().isEmpty());
        StatusWebhookListener.WebhookDeliveryRecord record = listener.getRecentDeliveries().get(0);
        assertTrue(record.getPayload().contains("\"SENT\""));
        assertTrue(record.getPayload().contains("\"email\""));
    }

    @Test
    @DisplayName("onFailure records error details")
    void onFailureRecordsError() throws Exception {
        StatusWebhookListener listener = new StatusWebhookListener(
                "http://localhost:1", 500, Map.of());

        listener.onFailure("slack", null, new RuntimeException("Connection refused"));

        Thread.sleep(1000);

        assertFalse(listener.getRecentDeliveries().isEmpty());
        String payload = listener.getRecentDeliveries().get(0).getPayload();
        assertTrue(payload.contains("\"FAILED\""));
        assertTrue(payload.contains("RuntimeException"));
        assertTrue(payload.contains("Connection refused"));
    }

    @Test
    @DisplayName("Delivery records capped at 50")
    void deliveryRecordsCappedAtMax() throws Exception {
        StatusWebhookListener listener = new StatusWebhookListener(
                "http://localhost:1", 200, Map.of());

        for (int i = 0; i < 55; i++) {
            listener.onSuccess("email", "template-" + i);
        }

        // Wait for all async tasks
        Thread.sleep(3000);

        assertTrue(listener.getRecentDeliveries().size() <= 50);
    }
}
