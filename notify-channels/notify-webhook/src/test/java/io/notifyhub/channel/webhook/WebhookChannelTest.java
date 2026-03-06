package io.notifyhub.channel.webhook;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationSendException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WebhookChannelTest {

    @Test
    @DisplayName("getName() returns configured channel name")
    void getName() {
        WebhookChannel channel = new WebhookChannel(
                WebhookConfig.builder()
                        .name("order-webhook")
                        .url("https://api.example.com/hook")
                        .build());
        assertEquals("order-webhook", channel.getName());
    }

    @Test
    @DisplayName("isAvailable() returns true when URL is set")
    void isAvailable() {
        WebhookChannel channel = new WebhookChannel(
                WebhookConfig.builder()
                        .name("test-webhook")
                        .url("https://api.example.com/hook")
                        .build());
        assertTrue(channel.isAvailable());
    }

    @Test
    @DisplayName("Config requires non-blank name")
    void configValidationName() {
        assertThrows(IllegalArgumentException.class, () ->
                WebhookConfig.builder().name("").url("https://example.com").build());
        assertThrows(IllegalArgumentException.class, () ->
                WebhookConfig.builder().url("https://example.com").build());
    }

    @Test
    @DisplayName("Config requires non-blank URL")
    void configValidationUrl() {
        assertThrows(IllegalArgumentException.class, () ->
                WebhookConfig.builder().name("hook").url("").build());
        assertThrows(IllegalArgumentException.class, () ->
                WebhookConfig.builder().name("hook").build());
    }

    @Test
    @DisplayName("Payload template renders placeholders correctly")
    void payloadTemplateRendering() {
        WebhookChannel channel = new WebhookChannel(
                WebhookConfig.builder()
                        .name("test")
                        .url("https://api.example.com/hook")
                        .build());

        String result = channel.renderPayload(
                "{\"recipient\":\"{{recipient}}\",\"subject\":\"{{subject}}\",\"content\":\"{{content}}\"}",
                "user@example.com",
                "Hello",
                "World",
                null);

        assertEquals("{\"recipient\":\"user@example.com\",\"subject\":\"Hello\",\"content\":\"World\"}", result);
    }

    @Test
    @DisplayName("Payload template escapes special JSON characters")
    void payloadTemplateEscaping() {
        WebhookChannel channel = new WebhookChannel(
                WebhookConfig.builder()
                        .name("test")
                        .url("https://api.example.com/hook")
                        .build());

        String result = channel.renderPayload(
                "{\"content\":\"{{content}}\"}",
                "user",
                "subject",
                "Line1\nLine2\twith \"quotes\"",
                null);

        assertEquals("{\"content\":\"Line1\\nLine2\\twith \\\"quotes\\\"\"}", result);
    }

    @Test
    @DisplayName("Payload template handles null values gracefully")
    void payloadTemplateNullValues() {
        WebhookChannel channel = new WebhookChannel(
                WebhookConfig.builder()
                        .name("test")
                        .url("https://api.example.com/hook")
                        .build());

        String result = channel.renderPayload(
                "{\"recipient\":\"{{recipient}}\",\"subject\":\"{{subject}}\",\"content\":\"{{content}}\"}",
                null,
                null,
                null,
                null);

        assertEquals("{\"recipient\":\"\",\"subject\":\"\",\"content\":\"\"}", result);
    }

    @Test
    @DisplayName("send() throws NotificationSendException on invalid URL")
    void sendFailsOnInvalidUrl() {
        WebhookChannel channel = new WebhookChannel(
                WebhookConfig.builder()
                        .name("test-webhook")
                        .url("https://invalid.example.com/webhook")
                        .build());

        Notification notification = new Notification(
                "user@example.com", "test-webhook", "Test Subject",
                null, "Test message", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }

    @Test
    @DisplayName("Config defaults are applied correctly")
    void configDefaults() {
        WebhookConfig config = WebhookConfig.builder()
                .name("default-test")
                .url("https://api.example.com/hook")
                .build();

        assertEquals("POST", config.getMethod());
        assertEquals(10_000, config.getTimeoutMs());
        assertTrue(config.getHeaders().isEmpty());
        assertTrue(config.getPayloadTemplate().contains("{{recipient}}"));
        assertTrue(config.getPayloadTemplate().contains("{{subject}}"));
        assertTrue(config.getPayloadTemplate().contains("{{content}}"));
    }

    @Test
    @DisplayName("Config custom values are preserved")
    void configCustomValues() {
        Map<String, String> headers = Map.of("Authorization", "Bearer token123");
        WebhookConfig config = WebhookConfig.builder()
                .name("custom")
                .url("https://api.example.com/hook")
                .method("PUT")
                .timeoutMs(5000)
                .headers(headers)
                .payloadTemplate("{\"msg\":\"{{content}}\"}")
                .build();

        assertEquals("custom", config.getName());
        assertEquals("https://api.example.com/hook", config.getUrl());
        assertEquals("PUT", config.getMethod());
        assertEquals(5000, config.getTimeoutMs());
        assertEquals("Bearer token123", config.getHeaders().get("Authorization"));
        assertEquals("{\"msg\":\"{{content}}\"}", config.getPayloadTemplate());
    }
}
