package io.notifyhub.spring.properties;

import java.util.LinkedHashMap;
import java.util.Map;

public class GoogleChatProperties {
    private String webhookUrl;
    private int timeoutMs = 10_000;
    private Map<String, String> recipients = new LinkedHashMap<>();
    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    public Map<String, String> getRecipients() { return recipients; }
    public void setRecipients(Map<String, String> recipients) { this.recipients = recipients; }
}
