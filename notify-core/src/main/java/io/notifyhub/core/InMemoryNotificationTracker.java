package io.notifyhub.core;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default in-memory implementation of {@link NotificationTracker}.
 * Stores delivery receipts in a thread-safe list.
 *
 * <p>Suitable for development, testing, and small-scale applications.
 * For production use with high volume, implement {@link NotificationTracker}
 * with a database backend.</p>
 *
 * <pre>{@code
 * NotificationTracker tracker = new InMemoryNotificationTracker();
 * NotifyHub notify = NotifyHub.builder()
 *     .channel(emailChannel)
 *     .tracker(tracker)
 *     .build();
 * }</pre>
 */
public class InMemoryNotificationTracker implements NotificationTracker {

    private final CopyOnWriteArrayList<DeliveryReceipt> receipts = new CopyOnWriteArrayList<>();

    @Override
    public void record(DeliveryReceipt receipt) {
        receipts.add(0, receipt); // newest first
    }

    @Override
    public Optional<DeliveryReceipt> findById(String id) {
        return receipts.stream()
                .filter(r -> r.getId().equals(id))
                .findFirst();
    }

    @Override
    public List<DeliveryReceipt> findByRecipient(String recipient) {
        return receipts.stream()
                .filter(r -> recipient.equals(r.getRecipient()))
                .toList();
    }

    @Override
    public List<DeliveryReceipt> findByStatus(DeliveryStatus status) {
        return receipts.stream()
                .filter(r -> r.getStatus() == status)
                .toList();
    }

    @Override
    public List<DeliveryReceipt> findAll() {
        return Collections.unmodifiableList(receipts);
    }

    @Override
    public long count() {
        return receipts.size();
    }

    @Override
    public void clear() {
        receipts.clear();
    }
}
