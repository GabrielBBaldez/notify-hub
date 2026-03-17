package io.notifyhub.core;

import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.event.EventType;
import io.notifyhub.core.event.NotificationEvent;
import io.notifyhub.core.event.NotificationEventBus;
import io.notifyhub.core.event.NotificationEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationSchedulerTest {

    @Mock
    private NotificationExecutor executor;

    @Mock
    private NotificationChannel emailChannel;

    @Mock
    private NotificationTracker tracker;

    private final List<NotificationEvent> capturedEvents = new ArrayList<>();
    private NotificationEventBus eventBus;
    private NotifyHub hub;

    @BeforeEach
    void setUp() {
        when(emailChannel.getName()).thenReturn("email");
        when(emailChannel.sendWithResult(any())).thenCallRealMethod();

        capturedEvents.clear();
        NotificationEventListener capturingListener = capturedEvents::add;
        eventBus = new NotificationEventBus(List.of(capturingListener));

        // Build a minimal hub just to create NotificationBuilder instances
        hub = NotifyHub.builder()
                .channel(emailChannel)
                .build();
    }

    @Test
    @DisplayName("Should publish SCHEDULED event and return ScheduledNotification")
    void schedulePublishesScheduledEventAndReturnsHandle() {
        ScheduledExecutorService scheduledExec = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "test-scheduler");
            t.setDaemon(true);
            return t;
        });

        try {
            NotificationScheduler scheduler = new NotificationScheduler(
                    executor, eventBus, scheduledExec, tracker
            );

            NotificationBuilder builder = hub.to("user@test.com")
                    .via(Channel.EMAIL)
                    .content("Hello scheduled!");

            ScheduledNotification result = scheduler.schedule(builder, Duration.ofHours(1));

            assertNotNull(result, "Should return a non-null ScheduledNotification");
            assertEquals("email", result.getChannelName());
            assertEquals("user@test.com", result.getRecipient());
            assertEquals(DeliveryStatus.SCHEDULED, result.getStatus());

            // Verify SCHEDULED event was published
            assertEquals(1, capturedEvents.size());
            NotificationEvent event = capturedEvents.get(0);
            assertEquals(EventType.SCHEDULED, event.type());
            assertEquals("email", event.channelName());
            assertEquals("user@test.com", event.recipient());

            // Verify tracker recorded SCHEDULED receipt
            verify(tracker).record(argThat(receipt ->
                    receipt.getStatus() == DeliveryStatus.SCHEDULED
                            && receipt.getChannelName().equals("email")
                            && receipt.getRecipient().equals("user@test.com")
            ));

            // Clean up: cancel to prevent execution
            result.cancel();
        } finally {
            scheduledExec.shutdownNow();
        }
    }

    @Test
    @DisplayName("Should publish CANCELLED event when notification is cancelled")
    void cancelPublishesCancelledEvent() {
        ScheduledExecutorService scheduledExec = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "test-scheduler");
            t.setDaemon(true);
            return t;
        });

        try {
            NotificationScheduler scheduler = new NotificationScheduler(
                    executor, eventBus, scheduledExec, null
            );

            NotificationBuilder builder = hub.to("user@test.com")
                    .via(Channel.EMAIL)
                    .content("Cancel me!");

            ScheduledNotification result = scheduler.schedule(builder, Duration.ofHours(1));

            // Clear the SCHEDULED event so we can check only CANCELLED
            capturedEvents.clear();

            boolean cancelled = result.cancel();
            assertTrue(cancelled, "Should successfully cancel");
            assertEquals(DeliveryStatus.CANCELLED, result.getStatus());

            // Verify CANCELLED event was published
            assertEquals(1, capturedEvents.size());
            NotificationEvent event = capturedEvents.get(0);
            assertEquals(EventType.CANCELLED, event.type());
            assertEquals("email", event.channelName());
            assertEquals("user@test.com", event.recipient());
        } finally {
            scheduledExec.shutdownNow();
        }
    }

    @Test
    @DisplayName("Should use provided ScheduledExecutorService instead of creating default")
    void usesProvidedScheduledExecutorService() {
        ScheduledExecutorService customScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "custom-scheduler");
            t.setDaemon(true);
            return t;
        });

        try {
            NotificationScheduler scheduler = new NotificationScheduler(
                    executor, eventBus, customScheduler, null
            );

            NotificationBuilder builder = hub.to("user@test.com")
                    .via(Channel.EMAIL)
                    .content("Custom scheduler test");

            // Schedule with a very short delay so it actually runs on the custom scheduler
            ScheduledNotification result = scheduler.schedule(builder, Duration.ofMillis(50));

            // Wait for execution
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Verify the executor was called (meaning our custom scheduler triggered it)
            verify(executor, timeout(500)).execute(any(NotificationBuilder.class));

            // The notification should have been marked as sent (not failed)
            assertTrue(result.isDone(), "Notification should have executed");
            assertEquals(DeliveryStatus.SENT, result.getStatus());
        } finally {
            customScheduler.shutdownNow();
        }
    }

    @Test
    @DisplayName("Should mark notification as FAILED when execution throws exception")
    void marksFailedWhenExecutionThrows() {
        ScheduledExecutorService scheduledExec = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "test-scheduler");
            t.setDaemon(true);
            return t;
        });

        try {
            doThrow(new RuntimeException("Delivery failed"))
                    .when(executor).execute(any(NotificationBuilder.class));

            NotificationScheduler scheduler = new NotificationScheduler(
                    executor, eventBus, scheduledExec, tracker
            );

            NotificationBuilder builder = hub.to("user@test.com")
                    .via(Channel.EMAIL)
                    .content("Will fail");

            ScheduledNotification result = scheduler.schedule(builder, Duration.ofMillis(50));

            // Wait for execution
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            assertTrue(result.isDone(), "Notification should have executed");
            assertEquals(DeliveryStatus.FAILED, result.getStatus());

            // Verify tracker recorded FAILED receipt
            verify(tracker, timeout(500)).record(argThat(receipt ->
                    receipt.getStatus() == DeliveryStatus.FAILED
                            && receipt.getErrorMessage().equals("Delivery failed")
            ));
        } finally {
            scheduledExec.shutdownNow();
        }
    }
}
