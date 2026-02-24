package io.notifyhub.core;

/**
 * Represents the delivery status of a notification.
 */
public enum DeliveryStatus {

    /** Notification created but not yet sent. */
    PENDING,

    /** Notification is scheduled for future delivery. */
    SCHEDULED,

    /** Notification was successfully sent through the channel. */
    SENT,

    /** Notification failed to send (all retries exhausted). */
    FAILED,

    /** Scheduled notification was cancelled before delivery. */
    CANCELLED
}
