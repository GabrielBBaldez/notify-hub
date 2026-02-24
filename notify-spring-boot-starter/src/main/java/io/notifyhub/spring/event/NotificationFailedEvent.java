package io.notifyhub.spring.event;

import org.springframework.context.ApplicationEvent;

/**
 * Spring application event published when a notification fails to send.
 *
 * <p>Listen to this event using {@code @EventListener}:</p>
 * <pre>{@code
 * @EventListener
 * public void onNotificationFailed(NotificationFailedEvent event) {
 *     log.error("Notification failed via {} using template {}: {}",
 *         event.getChannelName(), event.getTemplateName(), event.getError().getMessage());
 * }
 * }</pre>
 */
public class NotificationFailedEvent extends ApplicationEvent {

    private final String channelName;
    private final String templateName;
    private final Exception error;

    public NotificationFailedEvent(Object source, String channelName, String templateName, Exception error) {
        super(source);
        this.channelName = channelName;
        this.templateName = templateName;
        this.error = error;
    }

    public String getChannelName() { return channelName; }
    public String getTemplateName() { return templateName; }
    public Exception getError() { return error; }
}
