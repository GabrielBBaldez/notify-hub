package io.notifyhub.spring.event;

import io.notifyhub.core.NotificationListener;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;

/**
 * Notification listener that publishes Spring application events
 * for each notification lifecycle event.
 *
 * <p>Allows any Spring component to react to notification events
 * using standard {@code @EventListener} annotations.</p>
 */
public class SpringEventNotificationListener implements NotificationListener {

    private final ApplicationEventPublisher eventPublisher;

    public SpringEventNotificationListener(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void onSuccess(String channelName, String templateName) {
        eventPublisher.publishEvent(new NotificationSentEvent(this, channelName, templateName));
    }

    @Override
    public void onFailure(String channelName, String templateName, Exception error) {
        eventPublisher.publishEvent(new NotificationFailedEvent(this, channelName, templateName, error));
    }

    @Override
    public void onScheduled(String channelName, String recipient, Duration delay) {
        eventPublisher.publishEvent(new NotificationScheduledEvent(this, channelName, recipient, delay));
    }
}
