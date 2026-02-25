package io.notifyhub.channel.websocket;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * WebSocket channel configuration.
 *
 * <pre>{@code
 * WebSocketConfig config = WebSocketConfig.builder()
 *     .uri("wss://echo.example.com/ws")
 *     .header("Authorization", "Bearer token")
 *     .messageFormat("{\"type\":\"notification\",\"content\":\"{{content}}\"}")
 *     .build();
 * }</pre>
 */
public class WebSocketConfig {

    private final String uri;
    private final int timeoutMs;
    private final boolean reconnectEnabled;
    private final long reconnectDelayMs;
    private final int maxReconnectAttempts;
    private final Map<String, String> headers;
    private final String messageFormat;

    private WebSocketConfig(Builder builder) {
        this.uri = requireNonBlank(builder.uri, "WebSocket URI");
        this.timeoutMs = builder.timeoutMs;
        this.reconnectEnabled = builder.reconnectEnabled;
        this.reconnectDelayMs = builder.reconnectDelayMs;
        this.maxReconnectAttempts = builder.maxReconnectAttempts;
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(builder.headers));
        this.messageFormat = builder.messageFormat;
    }

    public String getUri() { return uri; }
    public int getTimeoutMs() { return timeoutMs; }
    public boolean isReconnectEnabled() { return reconnectEnabled; }
    public long getReconnectDelayMs() { return reconnectDelayMs; }
    public int getMaxReconnectAttempts() { return maxReconnectAttempts; }
    public Map<String, String> getHeaders() { return headers; }
    public String getMessageFormat() { return messageFormat; }

    public static Builder builder() {
        return new Builder();
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be null or blank");
        }
        return value;
    }

    public static class Builder {
        private String uri;
        private int timeoutMs = 10_000;
        private boolean reconnectEnabled = true;
        private long reconnectDelayMs = 5_000;
        private int maxReconnectAttempts = 3;
        private Map<String, String> headers = new LinkedHashMap<>();
        private String messageFormat = "{{content}}";

        public Builder uri(String uri) { this.uri = uri; return this; }
        public Builder timeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; return this; }
        public Builder reconnectEnabled(boolean reconnectEnabled) { this.reconnectEnabled = reconnectEnabled; return this; }
        public Builder reconnectDelayMs(long reconnectDelayMs) { this.reconnectDelayMs = reconnectDelayMs; return this; }
        public Builder maxReconnectAttempts(int maxReconnectAttempts) { this.maxReconnectAttempts = maxReconnectAttempts; return this; }
        public Builder headers(Map<String, String> headers) { this.headers = new LinkedHashMap<>(headers); return this; }
        public Builder header(String key, String value) { this.headers.put(key, value); return this; }
        public Builder messageFormat(String messageFormat) { this.messageFormat = messageFormat; return this; }

        public WebSocketConfig build() {
            return new WebSocketConfig(this);
        }
    }
}
