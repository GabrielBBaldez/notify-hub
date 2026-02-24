package io.notifyhub.core.dlq;

import java.util.List;
import java.util.Optional;

/**
 * Dead letter queue for notifications that failed after exhausting all retries.
 *
 * <p>Implementations should be thread-safe. Use {@link InMemoryDeadLetterQueue}
 * as the default, or provide a database-backed implementation for persistence.</p>
 */
public interface DeadLetterQueue {

    /** Add a failed notification to the queue. */
    void enqueue(DeadLetter deadLetter);

    /** Find a dead letter by ID. */
    Optional<DeadLetter> findById(String id);

    /** Get all dead letters, newest first. */
    List<DeadLetter> findAll();

    /** Get dead letters for a specific channel. */
    List<DeadLetter> findByChannel(String channelName);

    /** Remove and return the oldest dead letter (FIFO). */
    Optional<DeadLetter> dequeue();

    /** Remove a specific dead letter by ID. Returns true if found and removed. */
    boolean remove(String id);

    /** Total number of dead letters in the queue. */
    long count();

    /** Clear all dead letters. */
    void clear();
}
