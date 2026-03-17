package io.notifyhub.spring.properties;

import java.util.LinkedHashMap;
import java.util.Map;

public class YouTubeProperties {
    private String accessToken;
    private String refreshToken;
    private String clientId;
    private String clientSecret;
    private String channelId;
    private String liveChatId;
    private Map<String, String> recipients = new LinkedHashMap<>();
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }
    public String getLiveChatId() { return liveChatId; }
    public void setLiveChatId(String liveChatId) { this.liveChatId = liveChatId; }
    public Map<String, String> getRecipients() { return recipients; }
    public void setRecipients(Map<String, String> recipients) { this.recipients = recipients; }
}
