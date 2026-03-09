package io.notifyhub.mcp.tools;

import io.notifyhub.core.DeliveryStatus;
import io.notifyhub.core.ScheduledNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScheduledNotificationRegistryTest {

    private ScheduledNotificationRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ScheduledNotificationRegistry();
    }

    @Test
    @DisplayName("Should register and find by ID")
    void registerAndFindById() {
        ScheduledNotification sn = mockScheduled("test-id-1", false, false,
                Instant.now().plus(Duration.ofMinutes(30)));

        registry.register(sn);

        Optional<ScheduledNotification> found = registry.findById("test-id-1");
        assertTrue(found.isPresent());
        assertEquals("test-id-1", found.get().getId());
    }

    @Test
    @DisplayName("Should return empty for unknown ID")
    void findByIdReturnsEmptyForUnknown() {
        Optional<ScheduledNotification> found = registry.findById("nonexistent");
        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("Should list all entries")
    void findAllReturnsAll() {
        registry.register(mockScheduled("id-1", false, false, Instant.now()));
        registry.register(mockScheduled("id-2", false, false, Instant.now()));
        registry.register(mockScheduled("id-3", false, false, Instant.now()));

        assertEquals(3, registry.findAll().size());
    }

    @Test
    @DisplayName("Should evict stale completed entries older than 1 hour")
    void evictStaleRemovesOldCompletedEntries() {
        // Old completed entry (2 hours ago)
        registry.register(mockScheduled("old-done", true, false,
                Instant.now().minus(Duration.ofHours(2))));
        // Recent completed entry (5 minutes ago)
        registry.register(mockScheduled("recent-done", true, false,
                Instant.now().minus(Duration.ofMinutes(5))));
        // Pending entry
        registry.register(mockScheduled("pending", false, false,
                Instant.now().plus(Duration.ofMinutes(30))));

        assertEquals(3, registry.size());

        registry.evictStale();

        assertEquals(2, registry.size());
        assertTrue(registry.findById("old-done").isEmpty());
        assertTrue(registry.findById("recent-done").isPresent());
        assertTrue(registry.findById("pending").isPresent());
    }

    @Test
    @DisplayName("Should evict stale cancelled entries older than 1 hour")
    void evictStaleRemovesOldCancelledEntries() {
        registry.register(mockScheduled("old-cancelled", false, true,
                Instant.now().minus(Duration.ofHours(2))));
        registry.register(mockScheduled("pending", false, false,
                Instant.now().plus(Duration.ofMinutes(30))));

        registry.evictStale();

        assertEquals(1, registry.size());
        assertTrue(registry.findById("old-cancelled").isEmpty());
    }

    @Test
    @DisplayName("Should track size correctly")
    void sizeTracksCorrectly() {
        assertEquals(0, registry.size());

        registry.register(mockScheduled("id-1", false, false, Instant.now()));
        assertEquals(1, registry.size());

        registry.register(mockScheduled("id-2", false, false, Instant.now()));
        assertEquals(2, registry.size());
    }

    private ScheduledNotification mockScheduled(String id, boolean done, boolean cancelled,
                                                 Instant scheduledTime) {
        ScheduledNotification sn = mock(ScheduledNotification.class);
        when(sn.getId()).thenReturn(id);
        when(sn.isDone()).thenReturn(done);
        when(sn.isCancelled()).thenReturn(cancelled);
        when(sn.getScheduledTime()).thenReturn(scheduledTime);
        when(sn.getChannelName()).thenReturn("email");
        when(sn.getRecipient()).thenReturn("user@test.com");
        when(sn.getStatus()).thenReturn(
                cancelled ? DeliveryStatus.CANCELLED :
                done ? DeliveryStatus.SENT : DeliveryStatus.SCHEDULED);
        when(sn.getRemainingDelay()).thenReturn(done || cancelled ? Duration.ZERO : Duration.ofMinutes(30));
        return sn;
    }
}
