package io.notifyhub.channel.websocket;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationSendException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketChannelTest {

    @Test
    @DisplayName("getName() returns 'websocket'")
    void getName() {
        WebSocketChannel channel = new WebSocketChannel(
                WebSocketConfig.builder().uri("wss://echo.example.com/ws").build());
        assertEquals("websocket", channel.getName());
    }

    @Test
    @DisplayName("isAvailable() returns true when URI is set")
    void isAvailable() {
        WebSocketChannel channel = new WebSocketChannel(
                WebSocketConfig.builder().uri("wss://echo.example.com/ws").build());
        assertTrue(channel.isAvailable());
    }

    @Test
    @DisplayName("Config requires non-blank URI")
    void configValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                WebSocketConfig.builder().uri("").build());
        assertThrows(IllegalArgumentException.class, () ->
                WebSocketConfig.builder().build());
    }

    @Test
    @DisplayName("Config defaults are correct")
    void configDefaults() {
        WebSocketConfig config = WebSocketConfig.builder()
                .uri("wss://test.example.com/ws")
                .build();
        assertEquals(10_000, config.getTimeoutMs());
        assertTrue(config.isReconnectEnabled());
        assertEquals(5_000, config.getReconnectDelayMs());
        assertEquals(3, config.getMaxReconnectAttempts());
        assertTrue(config.getHeaders().isEmpty());
        assertEquals("{{content}}", config.getMessageFormat());
    }

    @Test
    @DisplayName("Config custom values are preserved")
    void configCustomValues() {
        WebSocketConfig config = WebSocketConfig.builder()
                .uri("wss://custom.example.com/ws")
                .timeoutMs(5000)
                .reconnectEnabled(false)
                .reconnectDelayMs(2000)
                .maxReconnectAttempts(5)
                .header("Authorization", "Bearer token123")
                .header("X-Custom", "value")
                .messageFormat("{\"text\":\"{{content}}\",\"to\":\"{{recipient}}\"}")
                .build();
        assertEquals("wss://custom.example.com/ws", config.getUri());
        assertEquals(5000, config.getTimeoutMs());
        assertFalse(config.isReconnectEnabled());
        assertEquals(2000, config.getReconnectDelayMs());
        assertEquals(5, config.getMaxReconnectAttempts());
        assertEquals(2, config.getHeaders().size());
        assertEquals("Bearer token123", config.getHeaders().get("Authorization"));
        assertEquals("{\"text\":\"{{content}}\",\"to\":\"{{recipient}}\"}", config.getMessageFormat());
    }

    @Test
    @DisplayName("send() throws NotificationSendException on invalid URI")
    void sendFailsOnInvalidUri() {
        WebSocketChannel channel = new WebSocketChannel(
                WebSocketConfig.builder()
                        .uri("wss://invalid.example.com/ws")
                        .reconnectEnabled(false)
                        .timeoutMs(2000)
                        .build());

        Notification notification = new Notification(
                "user@test.com", "websocket", null, null, "Test message", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }

    @Test
    @DisplayName("renderMessage() substitutes placeholders correctly")
    void messageFormatRendering() {
        WebSocketChannel channel = new WebSocketChannel(
                WebSocketConfig.builder()
                        .uri("wss://test.example.com/ws")
                        .messageFormat("{\"text\":\"{{content}}\",\"to\":\"{{recipient}}\",\"sub\":\"{{subject}}\"}")
                        .build());

        String result = channel.renderMessage("Hello World", "user@test.com", "Test Subject");
        assertEquals("{\"text\":\"Hello World\",\"to\":\"user@test.com\",\"sub\":\"Test Subject\"}", result);
    }

    @Test
    @DisplayName("renderMessage() handles null values gracefully")
    void messageFormatNullValues() {
        WebSocketChannel channel = new WebSocketChannel(
                WebSocketConfig.builder()
                        .uri("wss://test.example.com/ws")
                        .messageFormat("{\"text\":\"{{content}}\",\"to\":\"{{recipient}}\"}")
                        .build());

        String result = channel.renderMessage(null, null, null);
        assertEquals("{\"text\":\"\",\"to\":\"\"}", result);
    }

    @Test
    @DisplayName("escapeJson() escapes special characters correctly")
    void escapeJson() {
        assertEquals("Line1\\nLine2\\twith \\\"quotes\\\"",
                WebSocketChannel.escapeJson("Line1\nLine2\twith \"quotes\""));
        assertEquals("back\\\\slash", WebSocketChannel.escapeJson("back\\slash"));
        assertEquals("cr\\r", WebSocketChannel.escapeJson("cr\r"));
        assertEquals("", WebSocketChannel.escapeJson(null));
    }
}
