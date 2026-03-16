package io.notifyhub.channel.sns;

/**
 * AWS SNS notification channel configuration.
 *
 * <pre>{@code
 * AwsSnsConfig config = AwsSnsConfig.builder()
 *     .region("us-east-1")
 *     .accessKeyId("AKIA...")
 *     .secretAccessKey("secret")
 *     .topicArn("arn:aws:sns:us-east-1:123456789012:MyTopic")
 *     .build();
 * }</pre>
 */
public class AwsSnsConfig {

    private final String region;
    private final String accessKeyId;
    private final String secretAccessKey;
    private final String topicArn;

    private AwsSnsConfig(Builder builder) {
        this.region = requireNonBlank(builder.region, "region");
        this.accessKeyId = requireNonBlank(builder.accessKeyId, "accessKeyId");
        this.secretAccessKey = requireNonBlank(builder.secretAccessKey, "secretAccessKey");
        this.topicArn = builder.topicArn; // optional
    }

    public String getRegion() { return region; }
    public String getAccessKeyId() { return accessKeyId; }
    public String getSecretAccessKey() { return secretAccessKey; }
    public String getTopicArn() { return topicArn; }

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
        private String region;
        private String accessKeyId;
        private String secretAccessKey;
        private String topicArn;

        public Builder region(String region) { this.region = region; return this; }
        public Builder accessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; return this; }
        public Builder secretAccessKey(String secretAccessKey) { this.secretAccessKey = secretAccessKey; return this; }
        public Builder topicArn(String topicArn) { this.topicArn = topicArn; return this; }

        public AwsSnsConfig build() {
            return new AwsSnsConfig(this);
        }
    }
}
