package io.notifyhub.tracker.jpa;

import io.notifyhub.core.DeliveryReceipt;
import io.notifyhub.core.DeliveryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JpaNotificationTrackerTest {

    @Mock
    private DeliveryReceiptRepository repository;

    private JpaNotificationTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new JpaNotificationTracker(repository);
    }

    @Test
    @DisplayName("Should record a delivery receipt by saving entity to repository")
    void testRecord() {
        DeliveryReceipt receipt = DeliveryReceipt.builder()
                .id("receipt-1")
                .channelName("email")
                .recipient("user@test.com")
                .status(DeliveryStatus.SENT)
                .timestamp(Instant.parse("2024-03-15T10:30:00Z"))
                .build();

        tracker.record(receipt);

        ArgumentCaptor<DeliveryReceiptEntity> captor = ArgumentCaptor.forClass(DeliveryReceiptEntity.class);
        verify(repository).save(captor.capture());
        DeliveryReceiptEntity saved = captor.getValue();
        assertEquals("receipt-1", saved.getId());
        assertEquals("email", saved.getChannelName());
        assertEquals("user@test.com", saved.getRecipient());
        assertEquals(DeliveryStatus.SENT, saved.getStatus());
        assertEquals(Instant.parse("2024-03-15T10:30:00Z"), saved.getTimestamp());
    }

    @Test
    @DisplayName("Should find delivery receipt by ID when it exists")
    void testFindByIdFound() {
        DeliveryReceiptEntity entity = new DeliveryReceiptEntity(
                "receipt-1", "slack", "channel-123",
                DeliveryStatus.DELIVERED, Instant.parse("2024-03-15T10:30:00Z"),
                null, null);
        when(repository.findById("receipt-1")).thenReturn(Optional.of(entity));

        Optional<DeliveryReceipt> result = tracker.findById("receipt-1");

        assertTrue(result.isPresent());
        DeliveryReceipt receipt = result.get();
        assertEquals("receipt-1", receipt.getId());
        assertEquals("slack", receipt.getChannelName());
        assertEquals("channel-123", receipt.getRecipient());
        assertEquals(DeliveryStatus.DELIVERED, receipt.getStatus());
    }

    @Test
    @DisplayName("Should return empty Optional when receipt ID not found")
    void testFindByIdNotFound() {
        when(repository.findById("nonexistent")).thenReturn(Optional.empty());

        Optional<DeliveryReceipt> result = tracker.findById("nonexistent");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should find all receipts for a given recipient")
    void testFindByRecipient() {
        DeliveryReceiptEntity entity1 = new DeliveryReceiptEntity(
                "r-1", "email", "user@test.com",
                DeliveryStatus.SENT, Instant.parse("2024-03-15T10:00:00Z"),
                null, null);
        DeliveryReceiptEntity entity2 = new DeliveryReceiptEntity(
                "r-2", "sms", "user@test.com",
                DeliveryStatus.FAILED, Instant.parse("2024-03-15T11:00:00Z"),
                "Connection timeout", null);
        when(repository.findByRecipientOrderByTimestampDesc("user@test.com"))
                .thenReturn(List.of(entity2, entity1));

        List<DeliveryReceipt> results = tracker.findByRecipient("user@test.com");

        assertEquals(2, results.size());
        assertEquals("r-2", results.get(0).getId());
        assertEquals(DeliveryStatus.FAILED, results.get(0).getStatus());
        assertEquals("Connection timeout", results.get(0).getErrorMessage());
        assertEquals("r-1", results.get(1).getId());
        assertEquals(DeliveryStatus.SENT, results.get(1).getStatus());
    }

    @Test
    @DisplayName("Should find all receipts with a given status")
    void testFindByStatus() {
        DeliveryReceiptEntity entity = new DeliveryReceiptEntity(
                "r-1", "email", "user@test.com",
                DeliveryStatus.FAILED, Instant.parse("2024-03-15T10:00:00Z"),
                "SMTP error", "welcome-template");
        when(repository.findByStatusOrderByTimestampDesc(DeliveryStatus.FAILED))
                .thenReturn(List.of(entity));

        List<DeliveryReceipt> results = tracker.findByStatus(DeliveryStatus.FAILED);

        assertEquals(1, results.size());
        assertEquals("r-1", results.get(0).getId());
        assertEquals(DeliveryStatus.FAILED, results.get(0).getStatus());
        assertEquals("SMTP error", results.get(0).getErrorMessage());
        assertEquals("welcome-template", results.get(0).getTemplateName());
    }

    @Test
    @DisplayName("Should return all receipts ordered by timestamp descending")
    void testFindAll() {
        DeliveryReceiptEntity entity1 = new DeliveryReceiptEntity(
                "r-1", "email", "a@test.com",
                DeliveryStatus.SENT, Instant.parse("2024-03-15T09:00:00Z"),
                null, null);
        DeliveryReceiptEntity entity2 = new DeliveryReceiptEntity(
                "r-2", "slack", "channel-1",
                DeliveryStatus.SENT, Instant.parse("2024-03-15T10:00:00Z"),
                null, null);
        when(repository.findAllByOrderByTimestampDesc())
                .thenReturn(List.of(entity2, entity1));

        List<DeliveryReceipt> results = tracker.findAll();

        assertEquals(2, results.size());
        assertEquals("r-2", results.get(0).getId());
        assertEquals("r-1", results.get(1).getId());
    }

    @Test
    @DisplayName("Should return empty list when no receipts exist")
    void testFindAllEmpty() {
        when(repository.findAllByOrderByTimestampDesc()).thenReturn(List.of());

        List<DeliveryReceipt> results = tracker.findAll();

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should return count of receipts from repository")
    void testCount() {
        when(repository.count()).thenReturn(42L);

        long count = tracker.count();

        assertEquals(42L, count);
    }

    @Test
    @DisplayName("Should clear all receipts by calling deleteAll on repository")
    void testClear() {
        tracker.clear();

        verify(repository).deleteAll();
    }
}
