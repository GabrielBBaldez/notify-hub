package io.notifyhub.spring.properties;

import java.util.LinkedHashMap;
import java.util.Map;

public class KickProperties {
    private String clientId;
    private String accessToken;
    private String refreshToken;
    private String clientSecret;
    private String broadcasterId;
    private String messageType;
    private Map<String, String> recipients = new LinkedHashMap<>();
    private int timeoutMs = 10_000;

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    public String getBroadcasterId() { return broadcasterId; }
    public void setBroadcasterId(String broadcasterId) { this.broadcasterId = broadcasterId; }
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public Map<String, String> getRecipients() { return recipients; }
    public void setRecipients(Map<String, String> recipients) { this.recipients = recipients; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
}
