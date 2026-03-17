package io.notifyhub.core.pipeline;

import io.notifyhub.core.channel.NotificationSendException;
import io.notifyhub.core.channel.SendResult;
import io.notifyhub.core.event.EventType;
import io.notifyhub.core.event.NotificationEvent;
import io.notifyhub.core.event.NotificationEventBus;
import io.notifyhub.core.resilience.ChannelCircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pipeline handler that enforces per-channel circuit breaking.
 *
 * <p>If no {@link ChannelCircuitBreaker} is configured, this handler is a pass-through.
 * When the circuit is OPEN for a channel, a {@link NotificationSendException} is thrown
 * immediately without calling the next handler. On success the circuit is informed so it
 * can transition HALF_OPEN → CLOSED; on failure the circuit records the event so it can
 * transition CLOSED → OPEN or HALF_OPEN → OPEN.</p>
 */
class CircuitBreakerHandler implements SendHandler {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerHandler.class);

    private final ChannelCircuitBreaker circuitBreaker;
    private final NotificationEventBus eventBus;

    CircuitBreakerHandler(ChannelCircuitBreaker circuitBreaker, NotificationEventBus eventBus) {
        this.circuitBreaker = circuitBreaker;
        this.eventBus = eventBus;
    }

    @Override
    public SendResult handle(SendContext context, SendHandler next) {
        if (circuitBreaker == null) {
            return next.handle(context, null);
        }

        String channelName = context.getChannelName();

        if (!circuitBreaker.allowRequest(channelName)) {
            log.warn("Circuit breaker is OPEN for channel '{}' — rejecting request", channelName);
            if (eventBus != null) {
                eventBus.publish(NotificationEvent.builder(EventType.CIRCUIT_OPENED, channelName).build());
            }
            throw new NotificationSendException(channelName, "Circuit breaker is open");
        }

        try {
            SendResult result = next.handle(context, null);
            circuitBreaker.recordSuccess(channelName);
            return result;
        } catch (Exception e) {
            circuitBreaker.recordFailure(channelName);
            throw e;
        }
    }
}
