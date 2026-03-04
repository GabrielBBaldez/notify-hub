package io.notifyhub.core;

/**
 * Represents the delivery status of a notification.
 *
 * <p>Basic statuses (all channels):</p>
 * <ul>
 *     <li>{@link #PENDING} — Created but not yet sent</li>
 *     <li>{@link #SCHEDULED} — Scheduled for future delivery</li>
 *     <li>{@link #SENT} — Successfully sent through the channel</li>
 *     <li>{@link #FAILED} — Failed to send (all retries exhausted)</li>
 *     <li>{@link #CANCELLED} — Scheduled notification was cancelled</li>
 * </ul>
 *
 * <p>Email provider tracking statuses (SendGrid, Mailgun, SES, etc.):</p>
 * <ul>
 *     <li>{@link #DELIVERED} — Recipient's mail server accepted the email</li>
 *     <li>{@link #OPENED} — Recipient opened the email (tracking pixel fired)</li>
 *     <li>{@link #CLICKED} — Recipient clicked a link in the email</li>
 *     <li>{@link #BOUNCED} — Email bounced (invalid address or mailbox full)</li>
 *     <li>{@link #SPAM_COMPLAINT} — Recipient marked the email as spam</li>
 *     <li>{@link #DROPPED} — Provider dropped the email (suppression list, etc.)</li>
 *     <li>{@link #DEFERRED} — Delivery temporarily delayed by recipient server</li>
 * </ul>
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
    CANCELLED,

    // ── Email provider tracking statuses ──────────────────────

    /** Recipient's mail server accepted the email. */
    DELIVERED,

    /** Recipient opened the email (tracking pixel fired). */
    OPENED,

    /** Recipient clicked a link in the email. */
    CLICKED,

    /** Email bounced (invalid address or mailbox full). */
    BOUNCED,

    /** Recipient marked the email as spam. */
    SPAM_COMPLAINT,

    /** Provider dropped the email (suppression list, invalid, etc.). */
    DROPPED,

    /** Delivery temporarily delayed by recipient's mail server. */
    DEFERRED
}
