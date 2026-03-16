package io.notifyhub.channel.pagerduty;

import java.util.List;

/**
 * PagerDuty Events API v2 configuration.
 *
 * <pre>{@code
 * PagerDutyConfig config = PagerDutyConfig.builder()
 *     .routingKey("your-routing-key")
 *     .severity("critical")
 *     .build();
 * }</pre>
 */
public class PagerDutyConfig {

    private static final List<String> VALID_SEVERITIES = List.of("critical", "error", "warning", "info");

    private final String routingKey;
    private final String severity;
    private final int timeoutMs;

    private PagerDutyConfig(Builder builder) {
        this.routingKey = requireNonBlank(builder.routingKey, "PagerDuty routing key");
        this.severity = validateSeverity(builder.severity);
        this.timeoutMs = builder.timeoutMs;
    }

    public String getRoutingKey() { return routingKey; }
    public String getSeverity() { return severity; }
    public int getTimeoutMs() { return timeoutMs; }

    public static Builder builder() {
        return new Builder();
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be null or blank");
        }
        return value;
    }

    private static String validateSeverity(String severity) {
        if (severity == null || severity.isBlank()) {
            return "warning";
        }
        String lower = severity.toLowerCase();
        if (!VALID_SEVERITIES.contains(lower)) {
            throw new IllegalArgumentException(
                    "Invalid severity '" + severity + "'. Must be one of: " + VALID_SEVERITIES);
        }
        return lower;
    }

    public static class Builder {
        private String routingKey;
        private String severity = "warning";
        private int timeoutMs = 10_000;

        public Builder routingKey(String routingKey) { this.routingKey = routingKey; return this; }
        public Builder severity(String severity) { this.severity = severity; return this; }
        public Builder timeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; return this; }

        public PagerDutyConfig build() {
            return new PagerDutyConfig(this);
        }
    }
}
