package io.notifyhub.core;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable receipt for a sent notification, containing delivery status and metadata.
 *
 * <pre>{@code
 * DeliveryReceipt receipt = notify.to("user@test.com")
 *     .via(Channel.EMAIL)
 *     .content("Hello!")
 *     .sendTracked();
 *
 * System.out.println(receipt.getStatus());    // SENT
 * System.out.println(receipt.getId());         // unique UUID
 * System.out.println(receipt.getChannel());    // email
 * System.out.println(receipt.getTimestamp());  // 2024-03-15T10:30:00Z
 * }</pre>
 */
public class DeliveryReceipt {

    private final String id;
    private final String channelName;
    private final String recipient;
    private final DeliveryStatus status;
    private final Instant timestamp;
    private final String errorMessage;
    private final String templateName;
    private final String providerMessageId;

    private DeliveryReceipt(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.channelName = builder.channelName;
        this.recipient = builder.recipient;
        this.status = builder.status;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.errorMessage = builder.errorMessage;
        this.templateName = builder.templateName;
        this.providerMessageId = builder.providerMessageId;
    }

    /** Unique identifier for this delivery attempt. */
    public String getId() {
        return id;
    }

    /** The channel through which the notification was sent. */
    public String getChannelName() {
        return channelName;
    }

    /** The recipient address. */
    public String getRecipient() {
        return recipient;
    }

    /** Current delivery status. */
    public DeliveryStatus getStatus() {
        return status;
    }

    /** When this delivery event occurred. */
    public Instant getTimestamp() {
        return timestamp;
    }

    /** Error message if status is FAILED. May be null. */
    public String getErrorMessage() {
        return errorMessage;
    }

    /** The template used, if any. May be null. */
    public String getTemplateName() {
        return templateName;
    }

    /**
     * The provider-assigned message ID for delivery tracking.
     * Non-null when sent through an API provider (e.g., SendGrid).
     * Null for SMTP-sent emails.
     */
    public String getProviderMessageId() {
        return providerMessageId;
    }

    @Override
    public String toString() {
        return "DeliveryReceipt{" +
                "id='" + id + '\'' +
                ", channel='" + channelName + '\'' +
                ", recipient='" + recipient + '\'' +
                ", status=" + status +
                ", timestamp=" + timestamp +
                (errorMessage != null ? ", error='" + errorMessage + '\'' : "") +
                (providerMessageId != null ? ", providerMessageId='" + providerMessageId + '\'' : "") +
                '}';
    }

    // ===================== BUILDER =====================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String channelName;
        private String recipient;
        private DeliveryStatus status;
        private Instant timestamp;
        private String errorMessage;
        private String templateName;
        private String providerMessageId;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder channelName(String channelName) {
            this.channelName = channelName;
            return this;
        }

        public Builder recipient(String recipient) {
            this.recipient = recipient;
            return this;
        }

        public Builder status(DeliveryStatus status) {
            this.status = status;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder templateName(String templateName) {
            this.templateName = templateName;
            return this;
        }

        public Builder providerMessageId(String providerMessageId) {
            this.providerMessageId = providerMessageId;
            return this;
        }

        public DeliveryReceipt build() {
            return new DeliveryReceipt(this);
        }
    }
}
