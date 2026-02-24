package io.notifyhub.core;

import java.util.List;
import java.util.Optional;

/**
 * Interface for tracking notification delivery history.
 * Implement this to persist delivery receipts to a database, log system, etc.
 *
 * <p>A default in-memory implementation is provided by {@link InMemoryNotificationTracker}.</p>
 *
 * <pre>{@code
 * NotifyHub notify = NotifyHub.builder()
 *     .channel(emailChannel)
 *     .tracker(new MyDatabaseTracker())
 *     .build();
 *
 * DeliveryReceipt receipt = notify.to("user@test.com")
 *     .via(Channel.EMAIL)
 *     .content("Hello!")
 *     .sendTracked();
 *
 * // Later, query delivery history
 * Optional<DeliveryReceipt> found = notify.getTracker().findById(receipt.getId());
 * List<DeliveryReceipt> history = notify.getTracker().findByRecipient("user@test.com");
 * }</pre>
 */
public interface NotificationTracker {

    /**
     * Record a delivery receipt.
     *
     * @param receipt the delivery receipt to store
     */
    void record(DeliveryReceipt receipt);

    /**
     * Find a delivery receipt by its unique ID.
     *
     * @param id the receipt ID
     * @return the receipt if found
     */
    Optional<DeliveryReceipt> findById(String id);

    /**
     * Find all delivery receipts for a given recipient.
     *
     * @param recipient the recipient address
     * @return list of receipts, newest first
     */
    List<DeliveryReceipt> findByRecipient(String recipient);

    /**
     * Find all delivery receipts with a given status.
     *
     * @param status the delivery status to filter by
     * @return list of receipts matching the status
     */
    List<DeliveryReceipt> findByStatus(DeliveryStatus status);

    /**
     * Get all tracked delivery receipts.
     *
     * @return all receipts, newest first
     */
    List<DeliveryReceipt> findAll();

    /**
     * Get total count of tracked deliveries.
     *
     * @return the count
     */
    long count();

    /**
     * Clear all tracked receipts.
     */
    void clear();
}
