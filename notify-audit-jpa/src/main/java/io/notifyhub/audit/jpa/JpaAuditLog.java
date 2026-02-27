package io.notifyhub.audit.jpa;

import io.notifyhub.core.AuditEntry;
import io.notifyhub.core.AuditEventType;
import io.notifyhub.core.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * JPA-backed implementation of {@link AuditLog}.
 * Persists audit entries to a relational database via Spring Data JPA.
 *
 * <p>Activated when {@code notify.audit.type=jpa} is set in application properties.</p>
 */
public class JpaAuditLog implements AuditLog {

    private static final Logger log = LoggerFactory.getLogger(JpaAuditLog.class);

    private final AuditEntryRepository repository;

    public JpaAuditLog(AuditEntryRepository repository) {
        this.repository = repository;
    }

    @Override
    public void record(AuditEntry entry) {
        AuditEntryEntity entity = AuditEntryEntity.fromAuditEntry(entry);
        repository.save(entity);
        log.debug("Recorded audit entry: {} [{}] -> {}", entry.getId(), entry.getEventType(), entry.getChannelName());
    }

    @Override
    public List<AuditEntry> findAll() {
        return repository.findAllByOrderByTimestampDesc()
                .stream()
                .map(AuditEntryEntity::toAuditEntry)
                .toList();
    }

    @Override
    public List<AuditEntry> findByEventType(AuditEventType eventType) {
        return repository.findByEventTypeOrderByTimestampDesc(eventType)
                .stream()
                .map(AuditEntryEntity::toAuditEntry)
                .toList();
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public void clear() {
        repository.deleteAll();
        log.info("Cleared all audit entries from JPA store");
    }
}
