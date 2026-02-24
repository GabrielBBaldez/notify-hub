package io.notifyhub.spring.event;

import org.springframework.context.ApplicationEvent;

/**
 * Spring application event published when a notification is successfully sent.
 *
 * <p>Listen to this event using {@code @EventListener}:</p>
 * <pre>{@code
 * @EventListener
 * public void onNotificationSent(NotificationSentEvent event) {
 *     log.info("Notification sent via {} using template {}",
 *         event.getChannelName(), event.getTemplateName());
 * }
 * }</pre>
 */
public class NotificationSentEvent extends ApplicationEvent {

    private final String channelName;
    private final String templateName;

    public NotificationSentEvent(Object source, String channelName, String templateName) {
        super(source);
        this.channelName = channelName;
        this.templateName = templateName;
    }

    public String getChannelName() { return channelName; }
    public String getTemplateName() { return templateName; }
}
