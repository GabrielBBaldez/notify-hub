package io.notifyhub.tracker.jpa;

import io.notifyhub.core.DeliveryReceipt;
import io.notifyhub.core.DeliveryStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a notification delivery receipt.
 * Maps to the {@code notification_delivery_receipts} table.
 */
@Entity
@Table(name = "notification_delivery_receipts")
public class DeliveryReceiptEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "channel_name")
    private String channelName;

    @Column(name = "recipient")
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DeliveryStatus status;

    @Column(name = "timestamp")
    private Instant timestamp;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "template_name")
    private String templateName;

    protected DeliveryReceiptEntity() {
        // JPA requires a no-arg constructor
    }

    public DeliveryReceiptEntity(String id, String channelName, String recipient,
                                  DeliveryStatus status, Instant timestamp,
                                  String errorMessage, String templateName) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.channelName = channelName;
        this.recipient = recipient;
        this.status = status;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.errorMessage = errorMessage;
        this.templateName = templateName;
    }

    // ===================== GETTERS & SETTERS =====================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public DeliveryStatus getStatus() { return status; }
    public void setStatus(DeliveryStatus status) { this.status = status; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    // ===================== CONVERSION =====================

    /**
     * Converts this entity to a {@link DeliveryReceipt} domain object.
     */
    public DeliveryReceipt toDeliveryReceipt() {
        return DeliveryReceipt.builder()
                .id(this.id)
                .channelName(this.channelName)
                .recipient(this.recipient)
                .status(this.status)
                .timestamp(this.timestamp)
                .errorMessage(this.errorMessage)
                .templateName(this.templateName)
                .build();
    }

    /**
     * Creates an entity from a {@link DeliveryReceipt} domain object.
     */
    public static DeliveryReceiptEntity fromDeliveryReceipt(DeliveryReceipt receipt) {
        return new DeliveryReceiptEntity(
                receipt.getId(),
                receipt.getChannelName(),
                receipt.getRecipient(),
                receipt.getStatus(),
                receipt.getTimestamp(),
                receipt.getErrorMessage(),
                receipt.getTemplateName()
        );
    }
}
