package io.notifyhub.spring.properties;

import java.util.LinkedHashMap;
import java.util.Map;

public class InstagramProperties {
    private String accessToken;
    private String igUserId;
    private Map<String, String> recipients = new LinkedHashMap<>();
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getIgUserId() { return igUserId; }
    public void setIgUserId(String igUserId) { this.igUserId = igUserId; }
    public Map<String, String> getRecipients() { return recipients; }
    public void setRecipients(Map<String, String> recipients) { this.recipients = recipients; }
}
