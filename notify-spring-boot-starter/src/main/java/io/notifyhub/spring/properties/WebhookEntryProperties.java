package io.notifyhub.spring.properties;

import java.util.LinkedHashMap;
import java.util.Map;

public class WebhookEntryProperties {
    private String name;
    private String url;
    private String payloadTemplate;
    private Map<String, String> headers = new LinkedHashMap<>();
    private String method = "POST";
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getPayloadTemplate() { return payloadTemplate; }
    public void setPayloadTemplate(String payloadTemplate) { this.payloadTemplate = payloadTemplate; }
    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
}
