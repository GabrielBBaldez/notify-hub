package io.notifyhub.spring.properties;

import java.util.LinkedHashMap;
import java.util.Map;

public class WhatsAppProperties {
    // Twilio backend
    private String accountSid;
    private String authToken;
    private String fromNumber;
    // Cloud API backend (Meta Graph API)
    private String accessToken;
    private String phoneNumberId;
    private String apiVersion;
    private Map<String, String> recipients = new LinkedHashMap<>();

    public String getAccountSid() { return accountSid; }
    public void setAccountSid(String accountSid) { this.accountSid = accountSid; }
    public String getAuthToken() { return authToken; }
    public void setAuthToken(String authToken) { this.authToken = authToken; }
    public String getFromNumber() { return fromNumber; }
    public void setFromNumber(String fromNumber) { this.fromNumber = fromNumber; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getPhoneNumberId() { return phoneNumberId; }
    public void setPhoneNumberId(String phoneNumberId) { this.phoneNumberId = phoneNumberId; }
    public String getApiVersion() { return apiVersion; }
    public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }
    public Map<String, String> getRecipients() { return recipients; }
    public void setRecipients(Map<String, String> recipients) { this.recipients = recipients; }
}
