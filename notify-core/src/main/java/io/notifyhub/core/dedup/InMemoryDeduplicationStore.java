package io.notifyhub.core.dedup;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory deduplication store backed by a {@link ConcurrentHashMap}.
 *
 * <p>Entries expire after a configurable TTL (default 24 hours).
 * Lazy cleanup runs on every {@link #isDuplicate} call.</p>
 *
 * <pre>{@code
 * DeduplicationStore store = new InMemoryDeduplicationStore(Duration.ofHours(12));
 * }</pre>
 */
public class InMemoryDeduplicationStore implements DeduplicationStore {

    private final ConcurrentHashMap<String, Instant> entries = new ConcurrentHashMap<>();
    private final Duration ttl;

    /**
     * Create a store with the specified TTL.
     *
     * @param ttl how long entries remain valid before being considered expired
     */
    public InMemoryDeduplicationStore(Duration ttl) {
        this.ttl = ttl != null ? ttl : Duration.ofHours(24);
    }

    /** Create a store with default 24-hour TTL. */
    public InMemoryDeduplicationStore() {
        this(Duration.ofHours(24));
    }

    @Override
    public boolean isDuplicate(String deduplicationKey) {
        if (deduplicationKey == null || deduplicationKey.isBlank()) {
            return false;
        }

        // Lazy cleanup
        cleanupExpired();

        Instant sentAt = entries.get(deduplicationKey);
        if (sentAt == null) {
            return false;
        }

        // Check if expired
        if (Instant.now().isAfter(sentAt.plus(ttl))) {
            entries.remove(deduplicationKey);
            return false;
        }

        return true;
    }

    @Override
    public void markSent(String deduplicationKey) {
        if (deduplicationKey != null && !deduplicationKey.isBlank()) {
            entries.put(deduplicationKey, Instant.now());
        }
    }

    @Override
    public void remove(String deduplicationKey) {
        entries.remove(deduplicationKey);
    }

    @Override
    public void clear() {
        entries.clear();
    }

    @Override
    public long size() {
        cleanupExpired();
        return entries.size();
    }

    /** Get the configured TTL. */
    public Duration getTtl() {
        return ttl;
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        entries.entrySet().removeIf(entry ->
                now.isAfter(entry.getValue().plus(ttl)));
    }
}
