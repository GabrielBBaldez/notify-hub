package io.notifyhub.core;

import java.time.Duration;

/**
 * Notification listener that records audit log entries for all notification events.
 *
 * <pre>{@code
 * AuditLog auditLog = new InMemoryAuditLog();
 * NotifyHub notify = NotifyHub.builder()
 *     .channel(emailChannel)
 *     .listener(new AuditNotificationListener(auditLog))
 *     .build();
 * }</pre>
 */
public class AuditNotificationListener implements NotificationListener {

    private final AuditLog auditLog;

    public AuditNotificationListener(AuditLog auditLog) {
        this.auditLog = auditLog;
    }

    @Override
    public void onSuccess(String channelName, String templateName) {
        auditLog.record(AuditEntry.builder()
                .eventType(AuditEventType.NOTIFICATION_SENT)
                .channelName(channelName)
                .templateName(templateName)
                .build());
    }

    @Override
    public void onFailure(String channelName, String templateName, Exception error) {
        auditLog.record(AuditEntry.builder()
                .eventType(AuditEventType.NOTIFICATION_FAILED)
                .channelName(channelName)
                .templateName(templateName)
                .details(error != null ? error.getMessage() : null)
                .build());
    }

    @Override
    public void onScheduled(String channelName, String recipient, Duration delay) {
        auditLog.record(AuditEntry.builder()
                .eventType(AuditEventType.NOTIFICATION_SCHEDULED)
                .channelName(channelName)
                .recipient(recipient)
                .details("Delay: " + delay)
                .build());
    }

    @Override
    public void onCancelled(String channelName, String recipient) {
        auditLog.record(AuditEntry.builder()
                .eventType(AuditEventType.NOTIFICATION_CANCELLED)
                .channelName(channelName)
                .recipient(recipient)
                .build());
    }
}
