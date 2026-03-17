package io.notifyhub.spring.properties;

public class MailgunProperties {
    private String apiKey;
    private String domain;
    private String from;
    private String region = "US";
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
}
