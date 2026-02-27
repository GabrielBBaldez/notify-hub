package io.notifyhub.audit.jpa;

import io.notifyhub.core.AuditEntry;
import io.notifyhub.core.AuditEventType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing an audit log entry.
 * Maps to the {@code notification_audit_log} table.
 */
@Entity
@Table(name = "notification_audit_log")
public class AuditEntryEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private AuditEventType eventType;

    @Column(name = "channel_name")
    private String channelName;

    @Column(name = "recipient")
    private String recipient;

    @Column(name = "template_name")
    private String templateName;

    @Column(name = "details", length = 2000)
    private String details;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    protected AuditEntryEntity() {
        // JPA requires a no-arg constructor
    }

    public AuditEntryEntity(String id, AuditEventType eventType, String channelName,
                             String recipient, String templateName, String details,
                             Instant timestamp) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.eventType = eventType;
        this.channelName = channelName;
        this.recipient = recipient;
        this.templateName = templateName;
        this.details = details;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    // ===================== GETTERS & SETTERS =====================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public AuditEventType getEventType() { return eventType; }
    public void setEventType(AuditEventType eventType) { this.eventType = eventType; }

    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    // ===================== CONVERSION =====================

    /**
     * Converts this entity to an {@link AuditEntry} domain object.
     */
    public AuditEntry toAuditEntry() {
        return AuditEntry.builder()
                .id(this.id)
                .eventType(this.eventType)
                .channelName(this.channelName)
                .recipient(this.recipient)
                .templateName(this.templateName)
                .details(this.details)
                .timestamp(this.timestamp)
                .build();
    }

    /**
     * Creates an entity from an {@link AuditEntry} domain object.
     */
    public static AuditEntryEntity fromAuditEntry(AuditEntry entry) {
        return new AuditEntryEntity(
                entry.getId(),
                entry.getEventType(),
                entry.getChannelName(),
                entry.getRecipient(),
                entry.getTemplateName(),
                entry.getDetails(),
                entry.getTimestamp()
        );
    }
}
