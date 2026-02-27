package io.notifyhub.core;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit log entry representing a system event.
 *
 * <pre>{@code
 * AuditEntry entry = AuditEntry.builder()
 *     .eventType(AuditEventType.NOTIFICATION_SENT)
 *     .channelName("email")
 *     .recipient("user@test.com")
 *     .build();
 * }</pre>
 */
public class AuditEntry {

    private final String id;
    private final AuditEventType eventType;
    private final String channelName;
    private final String recipient;
    private final String templateName;
    private final String details;
    private final Instant timestamp;

    private AuditEntry(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.eventType = builder.eventType;
        this.channelName = builder.channelName;
        this.recipient = builder.recipient;
        this.templateName = builder.templateName;
        this.details = builder.details;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
    }

    public String getId() { return id; }
    public AuditEventType getEventType() { return eventType; }
    public String getChannelName() { return channelName; }
    public String getRecipient() { return recipient; }
    public String getTemplateName() { return templateName; }
    public String getDetails() { return details; }
    public Instant getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return "AuditEntry{" +
                "id='" + id + '\'' +
                ", event=" + eventType +
                ", channel='" + channelName + '\'' +
                ", recipient='" + recipient + '\'' +
                ", timestamp=" + timestamp +
                (details != null ? ", details='" + details + '\'' : "") +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private AuditEventType eventType;
        private String channelName;
        private String recipient;
        private String templateName;
        private String details;
        private Instant timestamp;

        public Builder id(String id) { this.id = id; return this; }
        public Builder eventType(AuditEventType eventType) { this.eventType = eventType; return this; }
        public Builder channelName(String channelName) { this.channelName = channelName; return this; }
        public Builder recipient(String recipient) { this.recipient = recipient; return this; }
        public Builder templateName(String templateName) { this.templateName = templateName; return this; }
        public Builder details(String details) { this.details = details; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }

        public AuditEntry build() {
            return new AuditEntry(this);
        }
    }
}
