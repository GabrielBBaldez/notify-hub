package io.notifyhub.core.dedup;

/**
 * Store for tracking sent notification keys to prevent duplicate delivery.
 *
 * <p>Implementations should be thread-safe for concurrent access.</p>
 *
 * <pre>{@code
 * DeduplicationStore store = new InMemoryDeduplicationStore(Duration.ofHours(24));
 *
 * if (!store.isDuplicate("order-123")) {
 *     sendNotification();
 *     store.markSent("order-123");
 * }
 * }</pre>
 */
public interface DeduplicationStore {

    /**
     * Check if a notification with the given key was already sent.
     *
     * @param deduplicationKey the unique key for this notification
     * @return true if a notification with this key was already sent (and not expired)
     */
    boolean isDuplicate(String deduplicationKey);

    /**
     * Mark a notification key as sent.
     *
     * @param deduplicationKey the unique key to record
     */
    void markSent(String deduplicationKey);

    /**
     * Remove a specific key from the store.
     *
     * @param deduplicationKey the key to remove
     */
    void remove(String deduplicationKey);

    /**
     * Clear all entries from the store.
     */
    void clear();

    /**
     * Get the total number of active (non-expired) entries.
     *
     * @return entry count
     */
    long size();
}
