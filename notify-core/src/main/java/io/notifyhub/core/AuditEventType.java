package io.notifyhub.core;

/**
 * Types of audit log events tracked by the system.
 */
public enum AuditEventType {
    NOTIFICATION_SENT,
    NOTIFICATION_FAILED,
    NOTIFICATION_SCHEDULED,
    NOTIFICATION_CANCELLED
}
