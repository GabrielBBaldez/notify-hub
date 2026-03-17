package io.notifyhub.spring.properties;

import java.util.LinkedHashMap;
import java.util.Map;

public class FacebookProperties {
    private String pageAccessToken;
    private String pageId;
    private Map<String, String> recipients = new LinkedHashMap<>();
    public String getPageAccessToken() { return pageAccessToken; }
    public void setPageAccessToken(String pageAccessToken) { this.pageAccessToken = pageAccessToken; }
    public String getPageId() { return pageId; }
    public void setPageId(String pageId) { this.pageId = pageId; }
    public Map<String, String> getRecipients() { return recipients; }
    public void setRecipients(Map<String, String> recipients) { this.recipients = recipients; }
}
