package io.notifyhub.audit.jpa;

import io.notifyhub.core.AuditEntry;
import io.notifyhub.core.AuditEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JpaAuditLogTest {

    @Mock
    private AuditEntryRepository repository;

    private JpaAuditLog auditLog;

    @BeforeEach
    void setUp() {
        auditLog = new JpaAuditLog(repository);
    }

    @Test
    @DisplayName("Should record an audit entry by saving entity to repository")
    void testRecord() {
        AuditEntry entry = AuditEntry.builder()
                .id("audit-1")
                .eventType(AuditEventType.NOTIFICATION_SENT)
                .channelName("email")
                .recipient("user@test.com")
                .templateName("welcome")
                .details("Sent successfully")
                .timestamp(Instant.parse("2024-03-15T10:30:00Z"))
                .build();

        auditLog.record(entry);

        ArgumentCaptor<AuditEntryEntity> captor = ArgumentCaptor.forClass(AuditEntryEntity.class);
        verify(repository).save(captor.capture());
        AuditEntryEntity saved = captor.getValue();
        assertEquals("audit-1", saved.getId());
        assertEquals(AuditEventType.NOTIFICATION_SENT, saved.getEventType());
        assertEquals("email", saved.getChannelName());
        assertEquals("user@test.com", saved.getRecipient());
        assertEquals("welcome", saved.getTemplateName());
        assertEquals("Sent successfully", saved.getDetails());
        assertEquals(Instant.parse("2024-03-15T10:30:00Z"), saved.getTimestamp());
    }

    @Test
    @DisplayName("Should return all audit entries ordered by timestamp descending")
    void testFindAll() {
        AuditEntryEntity entity1 = new AuditEntryEntity(
                "a-1", AuditEventType.NOTIFICATION_SENT, "email",
                "user1@test.com", null, null,
                Instant.parse("2024-03-15T09:00:00Z"));
        AuditEntryEntity entity2 = new AuditEntryEntity(
                "a-2", AuditEventType.NOTIFICATION_FAILED, "slack",
                "channel-1", null, "Webhook error",
                Instant.parse("2024-03-15T10:00:00Z"));
        when(repository.findAllByOrderByTimestampDesc())
                .thenReturn(List.of(entity2, entity1));

        List<AuditEntry> results = auditLog.findAll();

        assertEquals(2, results.size());
        assertEquals("a-2", results.get(0).getId());
        assertEquals(AuditEventType.NOTIFICATION_FAILED, results.get(0).getEventType());
        assertEquals("a-1", results.get(1).getId());
        assertEquals(AuditEventType.NOTIFICATION_SENT, results.get(1).getEventType());
    }

    @Test
    @DisplayName("Should return empty list when no audit entries exist")
    void testFindAllEmpty() {
        when(repository.findAllByOrderByTimestampDesc()).thenReturn(List.of());

        List<AuditEntry> results = auditLog.findAll();

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should find audit entries filtered by event type")
    void testFindByEventType() {
        AuditEntryEntity entity = new AuditEntryEntity(
                "a-1", AuditEventType.NOTIFICATION_FAILED, "sms",
                "+5511999990000", null, "Twilio error: invalid number",
                Instant.parse("2024-03-15T10:00:00Z"));
        when(repository.findByEventTypeOrderByTimestampDesc(AuditEventType.NOTIFICATION_FAILED))
                .thenReturn(List.of(entity));

        List<AuditEntry> results = auditLog.findByEventType(AuditEventType.NOTIFICATION_FAILED);

        assertEquals(1, results.size());
        AuditEntry result = results.get(0);
        assertEquals("a-1", result.getId());
        assertEquals(AuditEventType.NOTIFICATION_FAILED, result.getEventType());
        assertEquals("sms", result.getChannelName());
        assertEquals("+5511999990000", result.getRecipient());
        assertEquals("Twilio error: invalid number", result.getDetails());
    }

    @Test
    @DisplayName("Should find audit entries for scheduled notifications")
    void testFindByEventTypeScheduled() {
        AuditEntryEntity entity = new AuditEntryEntity(
                "a-2", AuditEventType.NOTIFICATION_SCHEDULED, "email",
                "user@test.com", "reminder-template", "Scheduled for 2024-03-20",
                Instant.parse("2024-03-15T10:00:00Z"));
        when(repository.findByEventTypeOrderByTimestampDesc(AuditEventType.NOTIFICATION_SCHEDULED))
                .thenReturn(List.of(entity));

        List<AuditEntry> results = auditLog.findByEventType(AuditEventType.NOTIFICATION_SCHEDULED);

        assertEquals(1, results.size());
        assertEquals(AuditEventType.NOTIFICATION_SCHEDULED, results.get(0).getEventType());
        assertEquals("reminder-template", results.get(0).getTemplateName());
    }

    @Test
    @DisplayName("Should return count of audit entries from repository")
    void testCount() {
        when(repository.count()).thenReturn(15L);

        long count = auditLog.count();

        assertEquals(15L, count);
    }

    @Test
    @DisplayName("Should clear all audit entries by calling deleteAll on repository")
    void testClear() {
        auditLog.clear();

        verify(repository).deleteAll();
    }
}
