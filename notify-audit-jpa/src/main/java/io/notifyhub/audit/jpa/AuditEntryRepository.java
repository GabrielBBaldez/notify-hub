package io.notifyhub.audit.jpa;

import io.notifyhub.core.AuditEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link AuditEntryEntity}.
 */
public interface AuditEntryRepository extends JpaRepository<AuditEntryEntity, String> {

    /**
     * Find all audit entries with a given event type, ordered by timestamp descending (newest first).
     */
    List<AuditEntryEntity> findByEventTypeOrderByTimestampDesc(AuditEventType eventType);

    /**
     * Find all audit entries ordered by timestamp descending (newest first).
     */
    List<AuditEntryEntity> findAllByOrderByTimestampDesc();
}
