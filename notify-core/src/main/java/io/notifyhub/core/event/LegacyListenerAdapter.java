package io.notifyhub.core.event;

import io.notifyhub.core.NotificationListener;
import io.notifyhub.core.channel.NotificationSendException;

import java.time.Duration;
import java.util.Objects;

/**
 * Bridges the new {@link NotificationEventBus} to the legacy {@link NotificationListener} interface.
 *
 * <p>Translates {@link NotificationEvent}s into the corresponding legacy callback methods:
 * <ul>
 *   <li>{@code SENT} → {@link NotificationListener#onSuccess(String, String)}</li>
 *   <li>{@code FAILED} → {@link NotificationListener#onFailure(String, String, Exception)}</li>
 *   <li>{@code SCHEDULED} → {@link NotificationListener#onScheduled(String, String, Duration)}</li>
 *   <li>{@code CANCELLED} → {@link NotificationListener#onCancelled(String, String)}</li>
 * </ul>
 * All other event types are silently ignored (no legacy equivalent).
 */
public class LegacyListenerAdapter implements NotificationEventListener {

    private final NotificationListener delegate;

    public LegacyListenerAdapter(NotificationListener delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    @Override
    public void onEvent(NotificationEvent event) {
        switch (event.type()) {
            case SENT -> delegate.onSuccess(event.channelName(), event.templateName());
            case FAILED -> delegate.onFailure(
                    event.channelName(),
                    event.templateName(),
                    new NotificationSendException(event.channelName(), event.errorMessage())
            );
            case SCHEDULED -> delegate.onScheduled(
                    event.channelName(),
                    event.recipient(),
                    event.latency() != null ? event.latency() : Duration.ZERO
            );
            case CANCELLED -> delegate.onCancelled(event.channelName(), event.recipient());
            default -> { /* no legacy equivalent */ }
        }
    }

    /**
     * Returns the wrapped legacy listener.
     */
    public NotificationListener getDelegate() {
        return delegate;
    }
}
