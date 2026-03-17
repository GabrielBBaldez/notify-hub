package io.notifyhub.core;

import io.notifyhub.core.event.EventType;
import io.notifyhub.core.event.NotificationEvent;
import io.notifyhub.core.event.NotificationEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handles scheduling of notifications for future delivery.
 *
 * <p>This class is package-private and intended to be used internally by
 * {@link NotifyHub}. It encapsulates all scheduling logic that was previously
 * embedded in the hub facade.</p>
 *
 * <p>Dependency flow (no circular references):</p>
 * <ul>
 *   <li>NotificationScheduler -> NotificationExecutor (calls executor.execute())</li>
 *   <li>NotificationScheduler -> NotificationEventBus (publishes SCHEDULED/CANCELLED events)</li>
 *   <li>NotificationExecutor has NO reference to NotificationScheduler</li>
 * </ul>
 */
class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    private final NotificationExecutor executor;
    private final NotificationEventBus eventBus;
    private final ScheduledExecutorService providedScheduler;
    private final NotificationTracker tracker;

    private volatile ScheduledExecutorService defaultScheduler;

    NotificationScheduler(
            NotificationExecutor executor,
            NotificationEventBus eventBus,
            ScheduledExecutorService scheduledExecutor,
            NotificationTracker tracker
    ) {
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus must not be null");
        this.providedScheduler = scheduledExecutor; // nullable — lazy default created if needed
        this.tracker = tracker; // nullable
    }

    /**
     * Schedule a notification for future delivery.
     *
     * @param builder the notification builder with all delivery details
     * @param delay   how long to wait before sending
     * @return a handle to inspect or cancel the scheduled notification
     */
    ScheduledNotification schedule(NotificationBuilder builder, Duration delay) {
        ScheduledExecutorService sched = getScheduler();
        String primaryChannel = builder.getChannels().get(0);
        String recipient = builder.resolveRecipient(primaryChannel);
        Instant scheduledTime = Instant.now().plus(delay);

        // Use AtomicReference to share the ScheduledNotification between caller and scheduled task
        var ref = new AtomicReference<ScheduledNotification>();

        // Schedule the actual execution
        ScheduledFuture<?> future = sched.schedule(() -> {
            ScheduledNotification sn = ref.get();
            try {
                executor.execute(builder);
                if (sn != null) sn.markSent();

                // Update tracker with SENT status
                if (tracker != null && sn != null) {
                    DeliveryReceipt sentReceipt = DeliveryReceipt.builder()
                            .id(sn.getId() + "-sent")
                            .channelName(primaryChannel)
                            .recipient(recipient)
                            .status(DeliveryStatus.SENT)
                            .templateName(builder.getTemplateName())
                            .build();
                    tracker.record(sentReceipt);
                }
            } catch (Exception e) {
                if (sn != null) sn.markFailed();
                log.error("Scheduled notification failed: {}", e.getMessage());

                // Update tracker with FAILED status
                if (tracker != null && sn != null) {
                    DeliveryReceipt failedReceipt = DeliveryReceipt.builder()
                            .id(sn.getId() + "-failed")
                            .channelName(primaryChannel)
                            .recipient(recipient)
                            .status(DeliveryStatus.FAILED)
                            .templateName(builder.getTemplateName())
                            .errorMessage(e.getMessage())
                            .build();
                    tracker.record(failedReceipt);
                }
            }
        }, delay.toMillis(), TimeUnit.MILLISECONDS);

        // Create the ScheduledNotification with the actual future and share it
        ScheduledNotification scheduled = new ScheduledNotification(
                future, scheduledTime, primaryChannel, recipient
        );
        ref.set(scheduled);

        // Record as SCHEDULED in tracker
        if (tracker != null) {
            DeliveryReceipt scheduledReceipt = DeliveryReceipt.builder()
                    .id(scheduled.getId())
                    .channelName(primaryChannel)
                    .recipient(recipient)
                    .status(DeliveryStatus.SCHEDULED)
                    .templateName(builder.getTemplateName())
                    .build();
            tracker.record(scheduledReceipt);
        }

        // Wire cancel callback to publish CANCELLED event via event bus
        scheduled.setCancelCallback(() -> {
            eventBus.publish(
                    NotificationEvent.builder(EventType.CANCELLED, primaryChannel)
                            .recipient(recipient)
                            .templateName(builder.getTemplateName())
                            .build()
            );
        });

        // Publish SCHEDULED event
        eventBus.publish(
                NotificationEvent.builder(EventType.SCHEDULED, primaryChannel)
                        .recipient(recipient)
                        .templateName(builder.getTemplateName())
                        .build()
        );

        return scheduled;
    }

    // ===================== INTERNAL =====================

    private ScheduledExecutorService getScheduler() {
        if (providedScheduler != null) {
            return providedScheduler;
        }
        if (defaultScheduler == null) {
            synchronized (this) {
                if (defaultScheduler == null) {
                    defaultScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                        Thread t = new Thread(r, "notifyhub-scheduler");
                        t.setDaemon(true);
                        return t;
                    });
                }
            }
        }
        return defaultScheduler;
    }
}
