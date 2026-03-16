package io.notifyhub.core.dlq;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe in-memory implementation of {@link DeadLetterQueue}.
 * Dead letters are stored newest first.
 *
 * <p>Suitable for development and testing. For production use,
 * implement a database-backed DeadLetterQueue.</p>
 */
public class InMemoryDeadLetterQueue implements DeadLetterQueue {

    private final CopyOnWriteArrayList<DeadLetter> deadLetters = new CopyOnWriteArrayList<>();

    @Override
    public void enqueue(DeadLetter deadLetter) {
        deadLetters.add(0, deadLetter); // newest first
    }

    @Override
    public Optional<DeadLetter> findById(String id) {
        return deadLetters.stream()
                .filter(dl -> dl.getId().equals(id))
                .findFirst();
    }

    @Override
    public List<DeadLetter> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(deadLetters));
    }

    @Override
    public List<DeadLetter> findByChannel(String channelName) {
        return deadLetters.stream()
                .filter(dl -> dl.getChannelName().equals(channelName))
                .toList();
    }

    @Override
    public synchronized Optional<DeadLetter> dequeue() {
        if (deadLetters.isEmpty()) {
            return Optional.empty();
        }
        // Remove the oldest (last in the list since we add newest first)
        int lastIndex = deadLetters.size() - 1;
        DeadLetter oldest = deadLetters.remove(lastIndex);
        return Optional.of(oldest);
    }

    @Override
    public boolean remove(String id) {
        return deadLetters.removeIf(dl -> dl.getId().equals(id));
    }

    @Override
    public long count() {
        return deadLetters.size();
    }

    @Override
    public void clear() {
        deadLetters.clear();
    }
}
