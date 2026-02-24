package io.notifyhub.spring.event;

import org.springframework.context.ApplicationEvent;

import java.time.Duration;

/**
 * Spring application event published when a notification is scheduled for future delivery.
 *
 * <p>Listen to this event using {@code @EventListener}:</p>
 * <pre>{@code
 * @EventListener
 * public void onNotificationScheduled(NotificationScheduledEvent event) {
 *     log.info("Notification scheduled for {} via {} in {}",
 *         event.getRecipient(), event.getChannelName(), event.getDelay());
 * }
 * }</pre>
 */
public class NotificationScheduledEvent extends ApplicationEvent {

    private final String channelName;
    private final String recipient;
    private final Duration delay;

    public NotificationScheduledEvent(Object source, String channelName, String recipient, Duration delay) {
        super(source);
        this.channelName = channelName;
        this.recipient = recipient;
        this.delay = delay;
    }

    public String getChannelName() { return channelName; }
    public String getRecipient() { return recipient; }
    public Duration getDelay() { return delay; }
}
