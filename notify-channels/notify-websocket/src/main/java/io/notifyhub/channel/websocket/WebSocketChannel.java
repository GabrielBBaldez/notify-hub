package io.notifyhub.channel.websocket;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket notification channel using JDK {@link java.net.http.WebSocket}.
 *
 * <p>Sends messages to a WebSocket server. Each {@link #send(Notification)} call
 * opens a connection, sends the message, and closes — unless the connection
 * is already open and healthy.</p>
 *
 * <pre>{@code
 * WebSocketChannel ws = new WebSocketChannel(
 *     WebSocketConfig.builder()
 *         .uri("wss://echo.example.com/ws")
 *         .header("Authorization", "Bearer token")
 *         .messageFormat("{\"type\":\"notify\",\"text\":\"{{content}}\"}")
 *         .build()
 * );
 * }</pre>
 */
public class WebSocketChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(WebSocketChannel.class);

    private final WebSocketConfig config;
    private final HttpClient httpClient;

    public WebSocketChannel(WebSocketConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();
    }

    @Override
    public String getName() {
        return "websocket";
    }

    @Override
    public void send(Notification notification) {
        String content = notification.getRenderedContent();
        String recipient = notification.getRecipient();
        String message = renderMessage(content, recipient, notification.getSubject());

        int attempts = config.isReconnectEnabled() ? Math.max(1, config.getMaxReconnectAttempts()) : 1;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                WebSocket.Builder wsBuilder = httpClient.newWebSocketBuilder();

                // Add custom headers
                for (Map.Entry<String, String> header : config.getHeaders().entrySet()) {
                    wsBuilder.header(header.getKey(), header.getValue());
                }

                // Connect
                WebSocket ws = wsBuilder.buildAsync(
                        URI.create(config.getUri()), new SimpleListener()
                ).get(config.getTimeoutMs(), TimeUnit.MILLISECONDS);

                // Send text
                ws.sendText(message, true)
                  .get(config.getTimeoutMs(), TimeUnit.MILLISECONDS);

                // Close gracefully
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "done")
                  .get(config.getTimeoutMs(), TimeUnit.MILLISECONDS);

                log.debug("WebSocket message sent to '{}' via {}", recipient, config.getUri());
                return; // success

            } catch (Exception e) {
                log.warn("WebSocket send attempt {}/{} failed: {}", attempt, attempts, e.getMessage());
                if (attempt < attempts) {
                    try {
                        Thread.sleep(config.getReconnectDelayMs());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new NotificationSendException("websocket",
                                "WebSocket reconnect interrupted", ie);
                    }
                } else {
                    throw new NotificationSendException("websocket",
                            "Failed to send WebSocket message after " + attempts + " attempt(s): " + e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public boolean isAvailable() {
        return config.getUri() != null && !config.getUri().isBlank();
    }

    /**
     * Render the message using the configured format template.
     * Supports placeholders: {@code {{content}}}, {@code {{recipient}}}, {@code {{subject}}}.
     */
    String renderMessage(String content, String recipient, String subject) {
        String msg = config.getMessageFormat();
        msg = msg.replace("{{content}}", escapeJson(content != null ? content : ""));
        msg = msg.replace("{{recipient}}", escapeJson(recipient != null ? recipient : ""));
        msg = msg.replace("{{subject}}", escapeJson(subject != null ? subject : ""));
        return msg;
    }

    static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    /**
     * Minimal WebSocket listener — logs incoming frames for debugging.
     */
    private static class SimpleListener implements WebSocket.Listener {
        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            log.trace("WebSocket received: {}", data);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            log.trace("WebSocket closed: {} {}", statusCode, reason);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.debug("WebSocket error: {}", error.getMessage());
        }
    }
}
