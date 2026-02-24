package io.notifyhub.core;

import java.time.Duration;

/**
 * Listener for notification events.
 * Implement this to log, monitor, or react to notification outcomes.
 *
 * <pre>{@code
 * NotifyHub notify = NotifyHub.builder()
 *     .channel(emailChannel)
 *     .listener(new NotificationListener() {
 *         public void onSuccess(String channel, String template) {
 *             metrics.increment("notifications.sent." + channel);
 *         }
 *         public void onFailure(String channel, String template, Exception error) {
 *             alertService.warn("Notification failed: " + channel);
 *         }
 *         public void onScheduled(String channel, String recipient, Duration delay) {
 *             log.info("Notification scheduled for {} via {} in {}", recipient, channel, delay);
 *         }
 *     })
 *     .build();
 * }</pre>
 */
public interface NotificationListener {

    /**
     * Called after a notification is successfully sent.
     *
     * @param channelName  the channel that sent the notification
     * @param templateName the template used (may be null)
     */
    default void onSuccess(String channelName, String templateName) {
    }

    /**
     * Called when a notification fails to send.
     *
     * @param channelName  the channel that failed
     * @param templateName the template used (may be null)
     * @param error        the exception that caused the failure
     */
    default void onFailure(String channelName, String templateName, Exception error) {
    }

    /**
     * Called when a notification is scheduled for future delivery.
     *
     * @param channelName the channel it will be sent through
     * @param recipient   the recipient address
     * @param delay       the delay before delivery
     */
    default void onScheduled(String channelName, String recipient, Duration delay) {
    }

    /**
     * Called when a scheduled notification is cancelled.
     *
     * @param channelName the channel it was going to be sent through
     * @param recipient   the recipient address
     */
    default void onCancelled(String channelName, String recipient) {
    }
}
