package io.notifyhub.core;

import java.util.List;

/**
 * Interface for recording and querying audit log entries.
 *
 * <pre>{@code
 * AuditLog auditLog = new InMemoryAuditLog();
 * NotifyHub notify = NotifyHub.builder()
 *     .channel(emailChannel)
 *     .auditLog(auditLog)
 *     .build();
 *
 * // Query audit history
 * List<AuditEntry> all = auditLog.findAll();
 * List<AuditEntry> failures = auditLog.findByEventType(AuditEventType.NOTIFICATION_FAILED);
 * }</pre>
 */
public interface AuditLog {

    /** Record an audit entry. */
    void record(AuditEntry entry);

    /** Get all audit entries, newest first. */
    List<AuditEntry> findAll();

    /** Get audit entries filtered by event type. */
    List<AuditEntry> findByEventType(AuditEventType eventType);

    /** Get total count of audit entries. */
    long count();

    /** Clear all audit entries. */
    void clear();
}
