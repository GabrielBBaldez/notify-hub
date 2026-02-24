package io.notifyhub.core.channel;

import io.notifyhub.core.Notification;

/**
 * Interface that all notification channels must implement.
 *
 * <p>Built-in implementations: SmtpEmailChannel, TwilioSmsChannel.</p>
 *
 * <p>Create custom channels by implementing this interface:</p>
 * <pre>{@code
 * public class SlackChannel implements NotificationChannel {
 *
 *     @Override
 *     public String getName() { return "slack"; }
 *
 *     @Override
 *     public void send(Notification notification) {
 *         slackClient.postMessage(
 *             notification.getRecipient(),
 *             notification.getRenderedContent()
 *         );
 *     }
 * }
 * }</pre>
 */
public interface NotificationChannel {

    /**
     * Unique channel name. Must match the channel reference used in the fluent API.
     * Examples: "email", "sms", "whatsapp", "push", "slack".
     */
    String getName();

    /**
     * Send the notification through this channel.
     *
     * @param notification fully built and rendered notification
     * @throws NotificationSendException if sending fails
     */
    void send(Notification notification);

    /**
     * Check if this channel is properly configured and operational.
     * Used by health checks. Default returns true.
     */
    default boolean isAvailable() {
        return true;
    }
}
