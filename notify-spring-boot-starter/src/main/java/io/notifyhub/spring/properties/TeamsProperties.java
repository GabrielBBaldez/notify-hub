package io.notifyhub.spring.properties;

import java.util.LinkedHashMap;
import java.util.Map;

public class TeamsProperties {
    private String webhookUrl;
    private Map<String, String> recipients = new LinkedHashMap<>();
    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
    public Map<String, String> getRecipients() { return recipients; }
    public void setRecipients(Map<String, String> recipients) { this.recipients = recipients; }
}
