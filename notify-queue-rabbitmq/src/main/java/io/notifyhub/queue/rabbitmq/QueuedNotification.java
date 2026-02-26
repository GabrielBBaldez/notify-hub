package io.notifyhub.queue.rabbitmq;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Serializable DTO representing a notification in the queue.
 * Captures all data needed to reconstruct and send via NotifyHub.
 */
public class QueuedNotification implements Serializable {

    private static final long serialVersionUID = 1L;

    private String recipient;
    private String channelName;
    private String subject;
    private String templateName;
    private String rawContent;
    private Map<String, Object> params;
    private String priority;
    private String deduplicationKey;
    private Instant createdAt;

    public QueuedNotification() {
        this.params = new HashMap<>();
        this.createdAt = Instant.now();
    }

    // --- Builder ---

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final QueuedNotification n = new QueuedNotification();

        public Builder recipient(String recipient) { n.recipient = recipient; return this; }
        public Builder channelName(String channelName) { n.channelName = channelName; return this; }
        public Builder subject(String subject) { n.subject = subject; return this; }
        public Builder templateName(String templateName) { n.templateName = templateName; return this; }
        public Builder rawContent(String rawContent) { n.rawContent = rawContent; return this; }
        public Builder params(Map<String, Object> params) { n.params = params != null ? params : new HashMap<>(); return this; }
        public Builder priority(String priority) { n.priority = priority; return this; }
        public Builder deduplicationKey(String key) { n.deduplicationKey = key; return this; }

        public QueuedNotification build() {
            if (n.recipient == null || n.recipient.isBlank()) {
                throw new IllegalArgumentException("recipient is required");
            }
            if (n.channelName == null || n.channelName.isBlank()) {
                throw new IllegalArgumentException("channelName is required");
            }
            if (n.rawContent == null && n.templateName == null) {
                throw new IllegalArgumentException("Either rawContent or templateName must be provided");
            }
            return n;
        }
    }

    // --- Getters & Setters (for Jackson) ---

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public String getRawContent() { return rawContent; }
    public void setRawContent(String rawContent) { this.rawContent = rawContent; }

    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getDeduplicationKey() { return deduplicationKey; }
    public void setDeduplicationKey(String deduplicationKey) { this.deduplicationKey = deduplicationKey; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "QueuedNotification{channel=" + channelName + ", recipient=" + recipient + ", createdAt=" + createdAt + "}";
    }
}
