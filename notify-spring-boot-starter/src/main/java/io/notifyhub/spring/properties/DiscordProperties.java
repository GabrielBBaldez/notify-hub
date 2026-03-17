package io.notifyhub.spring.properties;

import java.util.LinkedHashMap;
import java.util.Map;

public class DiscordProperties {
    private String webhookUrl;
    private String username;
    private String avatarUrl;
    private Map<String, String> recipients = new LinkedHashMap<>();
    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public Map<String, String> getRecipients() { return recipients; }
    public void setRecipients(Map<String, String> recipients) { this.recipients = recipients; }
}
