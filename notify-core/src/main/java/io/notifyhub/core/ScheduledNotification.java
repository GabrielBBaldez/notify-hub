package io.notifyhub.core;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

/**
 * Handle for a scheduled notification. Allows cancellation and status inspection.
 *
 * <pre>{@code
 * ScheduledNotification scheduled = notify.to("user@test.com")
 *     .via(Channel.EMAIL)
 *     .content("Reminder!")
 *     .schedule(Duration.ofMinutes(30));
 *
 * // Later...
 * scheduled.cancel();                    // cancel before delivery
 * scheduled.isCancelled();               // true
 * scheduled.getScheduledTime();          // when it was going to fire
 * scheduled.getRemainingDelay();         // time left until delivery
 * }</pre>
 */
public class ScheduledNotification {

    private final String id;
    private final ScheduledFuture<?> future;
    private final Instant scheduledTime;
    private final String channelName;
    private final String recipient;
    private volatile DeliveryStatus status;

    ScheduledNotification(ScheduledFuture<?> future, Instant scheduledTime,
                          String channelName, String recipient) {
        this.id = UUID.randomUUID().toString();
        this.future = future;
        this.scheduledTime = scheduledTime;
        this.channelName = channelName;
        this.recipient = recipient;
        this.status = DeliveryStatus.SCHEDULED;
    }

    /** Unique identifier for this scheduled notification. */
    public String getId() {
        return id;
    }

    /** The time at which the notification is scheduled to be sent. */
    public Instant getScheduledTime() {
        return scheduledTime;
    }

    /** The channel through which the notification will be sent. */
    public String getChannelName() {
        return channelName;
    }

    /** The recipient address. */
    public String getRecipient() {
        return recipient;
    }

    /** Current delivery status. */
    public DeliveryStatus getStatus() {
        return status;
    }

    /**
     * Get remaining delay before the notification fires.
     * Returns Duration.ZERO if already executed or cancelled.
     */
    public Duration getRemainingDelay() {
        if (future.isDone() || future.isCancelled()) {
            return Duration.ZERO;
        }
        long remainingMs = future.getDelay(java.util.concurrent.TimeUnit.MILLISECONDS);
        return Duration.ofMillis(Math.max(0, remainingMs));
    }

    /** Whether this notification has already been sent. */
    public boolean isDone() {
        return future.isDone();
    }

    /** Whether this notification was cancelled. */
    public boolean isCancelled() {
        return future.isCancelled();
    }

    /**
     * Cancel this scheduled notification before it fires.
     *
     * @return true if the notification was successfully cancelled
     */
    public boolean cancel() {
        boolean cancelled = future.cancel(false);
        if (cancelled) {
            this.status = DeliveryStatus.CANCELLED;
        }
        return cancelled;
    }

    /** Called internally when the notification is successfully sent. */
    void markSent() {
        this.status = DeliveryStatus.SENT;
    }

    /** Called internally when the notification fails. */
    void markFailed() {
        this.status = DeliveryStatus.FAILED;
    }

    @Override
    public String toString() {
        return "ScheduledNotification{" +
                "id='" + id + '\'' +
                ", channel='" + channelName + '\'' +
                ", recipient='" + recipient + '\'' +
                ", scheduledTime=" + scheduledTime +
                ", status=" + status +
                '}';
    }
}
