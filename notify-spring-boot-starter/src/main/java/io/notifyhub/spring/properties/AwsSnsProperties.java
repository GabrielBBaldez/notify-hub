package io.notifyhub.spring.properties;

public class AwsSnsProperties {
    private String region;
    private String accessKeyId;
    private String secretAccessKey;
    private String topicArn;
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getAccessKeyId() { return accessKeyId; }
    public void setAccessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; }
    public String getSecretAccessKey() { return secretAccessKey; }
    public void setSecretAccessKey(String secretAccessKey) { this.secretAccessKey = secretAccessKey; }
    public String getTopicArn() { return topicArn; }
    public void setTopicArn(String topicArn) { this.topicArn = topicArn; }
}
