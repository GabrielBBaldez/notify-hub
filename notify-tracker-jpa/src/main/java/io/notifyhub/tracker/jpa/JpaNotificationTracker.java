package io.notifyhub.tracker.jpa;

import io.notifyhub.core.DeliveryReceipt;
import io.notifyhub.core.DeliveryStatus;
import io.notifyhub.core.NotificationTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * JPA-backed implementation of {@link NotificationTracker}.
 * Persists delivery receipts to a relational database via Spring Data JPA.
 *
 * <p>Activated when {@code notify.tracking.type=jpa} is set in application properties.</p>
 */
public class JpaNotificationTracker implements NotificationTracker {

    private static final Logger log = LoggerFactory.getLogger(JpaNotificationTracker.class);

    private final DeliveryReceiptRepository repository;

    public JpaNotificationTracker(DeliveryReceiptRepository repository) {
        this.repository = repository;
    }

    @Override
    public void record(DeliveryReceipt receipt) {
        DeliveryReceiptEntity entity = DeliveryReceiptEntity.fromDeliveryReceipt(receipt);
        repository.save(entity);
        log.debug("Recorded delivery receipt: {} [{}] -> {}", receipt.getId(), receipt.getChannelName(), receipt.getStatus());
    }

    @Override
    public Optional<DeliveryReceipt> findById(String id) {
        return repository.findById(id)
                .map(DeliveryReceiptEntity::toDeliveryReceipt);
    }

    @Override
    public List<DeliveryReceipt> findByRecipient(String recipient) {
        return repository.findByRecipientOrderByTimestampDesc(recipient)
                .stream()
                .map(DeliveryReceiptEntity::toDeliveryReceipt)
                .toList();
    }

    @Override
    public List<DeliveryReceipt> findByStatus(DeliveryStatus status) {
        return repository.findByStatusOrderByTimestampDesc(status)
                .stream()
                .map(DeliveryReceiptEntity::toDeliveryReceipt)
                .toList();
    }

    @Override
    public List<DeliveryReceipt> findAll() {
        return repository.findAllByOrderByTimestampDesc()
                .stream()
                .map(DeliveryReceiptEntity::toDeliveryReceipt)
                .toList();
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public void clear() {
        repository.deleteAll();
        log.info("Cleared all delivery receipts from JPA store");
    }
}
