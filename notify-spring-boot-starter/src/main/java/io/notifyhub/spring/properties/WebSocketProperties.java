package io.notifyhub.spring.properties;

import java.util.LinkedHashMap;
import java.util.Map;

public class WebSocketProperties {
    private String uri;
    private int timeoutMs = 10_000;
    private boolean reconnectEnabled = true;
    private long reconnectDelayMs = 5_000;
    private int maxReconnectAttempts = 3;
    private Map<String, String> headers = new LinkedHashMap<>();
    private String messageFormat = "{{content}}";
    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    public boolean isReconnectEnabled() { return reconnectEnabled; }
    public void setReconnectEnabled(boolean reconnectEnabled) { this.reconnectEnabled = reconnectEnabled; }
    public long getReconnectDelayMs() { return reconnectDelayMs; }
    public void setReconnectDelayMs(long reconnectDelayMs) { this.reconnectDelayMs = reconnectDelayMs; }
    public int getMaxReconnectAttempts() { return maxReconnectAttempts; }
    public void setMaxReconnectAttempts(int maxReconnectAttempts) { this.maxReconnectAttempts = maxReconnectAttempts; }
    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    public String getMessageFormat() { return messageFormat; }
    public void setMessageFormat(String messageFormat) { this.messageFormat = messageFormat; }
}
