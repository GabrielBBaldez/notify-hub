package io.notifyhub.spring.properties;

import java.util.LinkedHashMap;
import java.util.Map;

public class SlackProperties {
    private String webhookUrl;
    private Map<String, String> recipients = new LinkedHashMap<>();
    private String username;
    private String iconUrl;
    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
    public Map<String, String> getRecipients() { return recipients; }
    public void setRecipients(Map<String, String> recipients) { this.recipients = recipients; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }
}
