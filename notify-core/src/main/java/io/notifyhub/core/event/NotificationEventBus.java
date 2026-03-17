package io.notifyhub.core.event;

import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationEventBus {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventBus.class);

    private final List<NotificationEventListener> listeners;

    public NotificationEventBus(List<NotificationEventListener> listeners) {
        this.listeners = Collections.unmodifiableList(List.copyOf(listeners));
    }

    public void publish(NotificationEvent event) {
        for (NotificationEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                log.warn("Listener {} threw exception handling event {}: {}",
                        listener.getClass().getSimpleName(), event.type(), e.getMessage(), e);
            }
        }
    }
}
