package io.notifyhub.core.dlq;

import java.time.Instant;
import java.util.UUID;

/**
 * A failed notification that has been moved to the dead letter queue
 * after exhausting all retry attempts.
 *
 * <pre>{@code
 * DeadLetterQueue dlq = hub.getDeadLetterQueue();
 * List<DeadLetter> failed = dlq.findAll();
 * for (DeadLetter dl : failed) {
 *     System.out.println(dl.getChannelName() + " -> " + dl.getRecipient());
 *     hub.reprocessDeadLetter(dl.getId());
 * }
 * }</pre>
 */
public final class DeadLetter {

    private final String id;
    private final String channelName;
    private final String recipient;
    private final String subject;
    private final String content;
    private final String templateName;
    private final String errorMessage;
    private final Instant failedAt;
    private final int attemptCount;

    private DeadLetter(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.channelName = builder.channelName;
        this.recipient = builder.recipient;
        this.subject = builder.subject;
        this.content = builder.content;
        this.templateName = builder.templateName;
        this.errorMessage = builder.errorMessage;
        this.failedAt = builder.failedAt != null ? builder.failedAt : Instant.now();
        this.attemptCount = builder.attemptCount;
    }

    public String getId() { return id; }
    public String getChannelName() { return channelName; }
    public String getRecipient() { return recipient; }
    public String getSubject() { return subject; }
    public String getContent() { return content; }
    public String getTemplateName() { return templateName; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getFailedAt() { return failedAt; }
    public int getAttemptCount() { return attemptCount; }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "DeadLetter{" +
                "id='" + id + '\'' +
                ", channel='" + channelName + '\'' +
                ", recipient='" + recipient + '\'' +
                ", error='" + errorMessage + '\'' +
                ", attempts=" + attemptCount +
                '}';
    }

    public static class Builder {
        private String id;
        private String channelName;
        private String recipient;
        private String subject;
        private String content;
        private String templateName;
        private String errorMessage;
        private Instant failedAt;
        private int attemptCount;

        public Builder id(String id) { this.id = id; return this; }
        public Builder channelName(String channelName) { this.channelName = channelName; return this; }
        public Builder recipient(String recipient) { this.recipient = recipient; return this; }
        public Builder subject(String subject) { this.subject = subject; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder templateName(String templateName) { this.templateName = templateName; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public Builder failedAt(Instant failedAt) { this.failedAt = failedAt; return this; }
        public Builder attemptCount(int attemptCount) { this.attemptCount = attemptCount; return this; }

        public DeadLetter build() {
            return new DeadLetter(this);
        }
    }
}
