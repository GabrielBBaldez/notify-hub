package io.notifyhub.spring.properties;

import java.util.LinkedHashMap;
import java.util.Map;

public class NotionProperties {
    private String apiKey;
    private String databaseId;
    private Map<String, String> recipients = new LinkedHashMap<>();
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getDatabaseId() { return databaseId; }
    public void setDatabaseId(String databaseId) { this.databaseId = databaseId; }
    public Map<String, String> getRecipients() { return recipients; }
    public void setRecipients(Map<String, String> recipients) { this.recipients = recipients; }
}
