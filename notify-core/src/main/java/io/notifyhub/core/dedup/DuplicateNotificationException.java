package io.notifyhub.core.dedup;

/**
 * Thrown when a duplicate notification is detected and the system is configured
 * to throw on duplicates rather than silently skip.
 */
public class DuplicateNotificationException extends RuntimeException {

    private final String deduplicationKey;

    public DuplicateNotificationException(String deduplicationKey) {
        super("Duplicate notification detected (key: " + deduplicationKey + ")");
        this.deduplicationKey = deduplicationKey;
    }

    /** Get the deduplication key that was detected as a duplicate. */
    public String getDeduplicationKey() {
        return deduplicationKey;
    }
}
