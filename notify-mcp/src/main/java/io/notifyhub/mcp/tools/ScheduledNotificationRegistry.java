package io.notifyhub.mcp.tools;

import io.notifyhub.core.ScheduledNotification;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry for tracking scheduled notifications within the MCP server lifecycle.
 * Provides lookup by ID, listing, and lazy eviction of stale entries.
 */
public class ScheduledNotificationRegistry {

    private static final Duration COMPLETED_RETENTION = Duration.ofHours(1);

    private final ConcurrentHashMap<String, ScheduledNotification> entries = new ConcurrentHashMap<>();

    /** Register a scheduled notification for tracking. */
    public void register(ScheduledNotification notification) {
        entries.put(notification.getId(), notification);
    }

    /** Look up a scheduled notification by ID. */
    public Optional<ScheduledNotification> findById(String id) {
        return Optional.ofNullable(entries.get(id));
    }

    /** Return a snapshot of all tracked entries. */
    public List<ScheduledNotification> findAll() {
        return new ArrayList<>(entries.values());
    }

    /**
     * Remove completed/cancelled entries older than the retention period.
     * Called lazily before listing to avoid unbounded memory growth.
     */
    public void evictStale() {
        Instant cutoff = Instant.now().minus(COMPLETED_RETENTION);
        entries.entrySet().removeIf(e -> {
            ScheduledNotification sn = e.getValue();
            return (sn.isDone() || sn.isCancelled())
                    && sn.getScheduledTime().isBefore(cutoff);
        });
    }

    /** Current number of tracked entries. */
    public int size() {
        return entries.size();
    }
}
