package io.notifyhub.spring.properties;

public class PagerDutyProperties {
    private String routingKey;
    private String severity;
    public String getRoutingKey() { return routingKey; }
    public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
}
