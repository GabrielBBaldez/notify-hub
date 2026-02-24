package io.notifyhub.core;

/**
 * Notification priority levels.
 *
 * <p>Priority affects rate limiting behavior: {@link #URGENT} notifications
 * bypass rate limits entirely. Other priorities are handled normally.</p>
 *
 * <pre>{@code
 * notify.to(user)
 *     .via(Channel.EMAIL)
 *     .priority(Priority.URGENT)
 *     .content("Server is down!")
 *     .send();
 * }</pre>
 */
public enum Priority {

    /** Critical — bypasses rate limiting. */
    URGENT(0),

    /** Important but not critical. */
    HIGH(1),

    /** Default priority. */
    NORMAL(2),

    /** Low priority, can be deferred. */
    LOW(3);

    private final int level;

    Priority(int level) {
        this.level = level;
    }

    /** Numeric level (lower = higher priority). */
    public int getLevel() {
        return level;
    }

    /** Returns true if this priority is higher than the other. */
    public boolean isHigherThan(Priority other) {
        return this.level < other.level;
    }
}
