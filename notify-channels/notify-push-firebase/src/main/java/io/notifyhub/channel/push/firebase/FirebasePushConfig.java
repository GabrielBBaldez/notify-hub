package io.notifyhub.channel.push.firebase;

/**
 * Firebase Cloud Messaging (FCM) configuration using the HTTP v1 API.
 *
 * <pre>{@code
 * FirebasePushConfig config = FirebasePushConfig.builder()
 *     .projectId("my-firebase-project")
 *     .serviceAccountJson("{...}")
 *     .build();
 * }</pre>
 */
public class FirebasePushConfig {

    private final String projectId;
    private final String serviceAccountJson;
    private final int timeoutMs;

    private FirebasePushConfig(Builder builder) {
        this.projectId = requireNonBlank(builder.projectId, "FCM project ID");
        this.serviceAccountJson = requireNonBlank(builder.serviceAccountJson, "FCM service account JSON");
        this.timeoutMs = builder.timeoutMs;
    }

    public String getProjectId() { return projectId; }
    public String getServiceAccountJson() { return serviceAccountJson; }
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
        private String projectId;
        private String serviceAccountJson;
        private int timeoutMs = 10_000;

        public Builder projectId(String projectId) { this.projectId = projectId; return this; }
        public Builder serviceAccountJson(String serviceAccountJson) { this.serviceAccountJson = serviceAccountJson; return this; }
        public Builder timeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; return this; }

        public FirebasePushConfig build() {
            return new FirebasePushConfig(this);
        }
    }
}
