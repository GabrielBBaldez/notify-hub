package io.notifyhub.spring.properties;

import java.util.LinkedHashMap;
import java.util.Map;

public class LinkedInProperties {
    private String accessToken;
    private String authorId;
    private Map<String, String> recipients = new LinkedHashMap<>();
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public Map<String, String> getRecipients() { return recipients; }
    public void setRecipients(Map<String, String> recipients) { this.recipients = recipients; }
}
