package io.notifyhub.core;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default in-memory implementation of {@link AuditLog}.
 * Stores audit entries in a thread-safe list, newest first.
 *
 * <p>Suitable for development and testing. For production use,
 * implement {@link AuditLog} with a database backend.</p>
 */
public class InMemoryAuditLog implements AuditLog {

    private final CopyOnWriteArrayList<AuditEntry> entries = new CopyOnWriteArrayList<>();

    @Override
    public void record(AuditEntry entry) {
        entries.add(0, entry);
    }

    @Override
    public List<AuditEntry> findAll() {
        return Collections.unmodifiableList(entries);
    }

    @Override
    public List<AuditEntry> findByEventType(AuditEventType eventType) {
        return entries.stream()
                .filter(e -> e.getEventType() == eventType)
                .toList();
    }

    @Override
    public long count() {
        return entries.size();
    }

    @Override
    public void clear() {
        entries.clear();
    }
}
