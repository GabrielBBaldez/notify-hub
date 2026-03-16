package io.notifyhub.channel.whatsapp;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * WhatsApp Cloud API configuration (Meta Graph API).
 *
 * <pre>{@code
 * WhatsAppCloudConfig config = WhatsAppCloudConfig.builder()
 *     .accessToken("EAAGm0PX4ZCps...")
 *     .phoneNumberId("106540012345678")
 *     .build();
 * }</pre>
 */
public class WhatsAppCloudConfig {

    private final String accessToken;
    private final String phoneNumberId;
    private final Map<String, String> recipients;
    private final int timeoutMs;
    private final String apiVersion;

    private WhatsAppCloudConfig(Builder builder) {
        this.accessToken = requireNonBlank(builder.accessToken, "WhatsApp Cloud API access token");
        this.phoneNumberId = requireNonBlank(builder.phoneNumberId, "WhatsApp Cloud API phone number ID");
        this.recipients = builder.recipients != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(builder.recipients))
                : Collections.emptyMap();
        this.timeoutMs = builder.timeoutMs;
        this.apiVersion = builder.apiVersion;
    }

    public String getAccessToken() { return accessToken; }
    public String getPhoneNumberId() { return phoneNumberId; }
    public Map<String, String> getRecipients() { return recipients; }
    public int getTimeoutMs() { return timeoutMs; }
    public String getApiVersion() { return apiVersion; }

    public static Builder builder() {
        return new Builder();
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be null or blank");
        }
        return value;
    }

    public static class Builder {
        private String accessToken;
        private String phoneNumberId;
        private Map<String, String> recipients;
        private int timeoutMs = 10_000;
        private String apiVersion = "v21.0";

        public Builder accessToken(String accessToken) { this.accessToken = accessToken; return this; }
        public Builder phoneNumberId(String phoneNumberId) { this.phoneNumberId = phoneNumberId; return this; }
        public Builder recipients(Map<String, String> recipients) { this.recipients = recipients; return this; }
        public Builder timeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; return this; }
        public Builder apiVersion(String apiVersion) { this.apiVersion = apiVersion; return this; }

        public WhatsAppCloudConfig build() {
            return new WhatsAppCloudConfig(this);
        }
    }
}
