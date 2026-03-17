package io.notifyhub.spring.properties;

import java.util.LinkedHashMap;
import java.util.Map;

public class TikTokShopProperties {
    private String appKey;
    private String appSecret;
    private String accessToken;
    private String shopId;
    private Map<String, String> recipients = new LinkedHashMap<>();
    public String getAppKey() { return appKey; }
    public void setAppKey(String appKey) { this.appKey = appKey; }
    public String getAppSecret() { return appSecret; }
    public void setAppSecret(String appSecret) { this.appSecret = appSecret; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getShopId() { return shopId; }
    public void setShopId(String shopId) { this.shopId = shopId; }
    public Map<String, String> getRecipients() { return recipients; }
    public void setRecipients(Map<String, String> recipients) { this.recipients = recipients; }
}
