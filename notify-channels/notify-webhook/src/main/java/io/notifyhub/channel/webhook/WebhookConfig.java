package io.notifyhub.channel.webhook;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generic webhook configuration.
 *
 * <pre>{@code
 * WebhookConfig config = WebhookConfig.builder()
 *     .name("order-webhook")
 *     .url("https://api.example.com/notifications")
 *     .headers(Map.of("Authorization", "Bearer token123"))
 *     .build();
 * }</pre>
 */
public class WebhookConfig {

    private static final String DEFAULT_PAYLOAD_TEMPLATE =
            "{\"recipient\":\"{{recipient}}\",\"subject\":\"{{subject}}\",\"content\":\"{{content}}\"}";

    private final String name;
    private final String url;
    private final String payloadTemplate;
    private final Map<String, String> headers;
    private final String method;
    private final int timeoutMs;

    private WebhookConfig(Builder builder) {
        this.name = requireNonBlank(builder.name, "Webhook channel name");
        this.url = requireNonBlank(builder.url, "Webhook URL");
        this.payloadTemplate = builder.payloadTemplate;
        this.headers = builder.headers != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(builder.headers))
                : Collections.emptyMap();
        this.method = builder.method;
        this.timeoutMs = builder.timeoutMs;
    }

    public String getName() { return name; }
    public String getUrl() { return url; }
    public String getPayloadTemplate() { return payloadTemplate; }
    public Map<String, String> getHeaders() { return headers; }
    public String getMethod() { return method; }
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

    public static class Builder {
        private String name;
        private String url;
        private String payloadTemplate = DEFAULT_PAYLOAD_TEMPLATE;
        private Map<String, String> headers;
        private String method = "POST";
        private int timeoutMs = 10_000;

        public Builder name(String name) { this.name = name; return this; }
        public Builder url(String url) { this.url = url; return this; }
        public Builder payloadTemplate(String payloadTemplate) { this.payloadTemplate = payloadTemplate; return this; }
        public Builder headers(Map<String, String> headers) { this.headers = headers; return this; }
        public Builder method(String method) { this.method = method; return this; }
        public Builder timeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; return this; }

        public WebhookConfig build() {
            return new WebhookConfig(this);
        }
    }
}
