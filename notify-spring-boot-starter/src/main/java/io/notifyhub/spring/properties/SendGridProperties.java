package io.notifyhub.spring.properties;

public class SendGridProperties {
    private String apiKey;
    private String from;
    private String fromName;
    private boolean trackOpens = true;
    private boolean trackClicks = true;
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }
    public boolean isTrackOpens() { return trackOpens; }
    public void setTrackOpens(boolean trackOpens) { this.trackOpens = trackOpens; }
    public boolean isTrackClicks() { return trackClicks; }
    public void setTrackClicks(boolean trackClicks) { this.trackClicks = trackClicks; }
}
