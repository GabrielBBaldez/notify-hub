package io.notifyhub.tracker.jpa;

import io.notifyhub.core.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link DeliveryReceiptEntity}.
 */
public interface DeliveryReceiptRepository extends JpaRepository<DeliveryReceiptEntity, String> {

    /**
     * Find all receipts for a given recipient, ordered by timestamp descending (newest first).
     */
    List<DeliveryReceiptEntity> findByRecipientOrderByTimestampDesc(String recipient);

    /**
     * Find all receipts with a given status, ordered by timestamp descending (newest first).
     */
    List<DeliveryReceiptEntity> findByStatusOrderByTimestampDesc(DeliveryStatus status);

    /**
     * Find all receipts ordered by timestamp descending (newest first).
     */
    List<DeliveryReceiptEntity> findAllByOrderByTimestampDesc();
}
