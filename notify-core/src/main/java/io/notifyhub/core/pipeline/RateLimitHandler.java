package io.notifyhub.core.pipeline;

import io.notifyhub.core.Priority;
import io.notifyhub.core.channel.SendResult;
import io.notifyhub.core.event.EventType;
import io.notifyhub.core.event.NotificationEvent;
import io.notifyhub.core.event.NotificationEventBus;
import io.notifyhub.core.ratelimit.RateLimitExceededException;
import io.notifyhub.core.ratelimit.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pipeline handler that enforces per-channel rate limits.
 *
 * <p>If no {@link RateLimiter} is configured, this handler is a pass-through.
 * {@link Priority#URGENT} notifications bypass rate limiting entirely.
 * If the rate limit is exceeded, a {@link RateLimitExceededException} is thrown.</p>
 */
class RateLimitHandler implements SendHandler {

    private static final Logger log = LoggerFactory.getLogger(RateLimitHandler.class);

    private final RateLimiter rateLimiter;
    private final NotificationEventBus eventBus;

    RateLimitHandler(RateLimiter rateLimiter, NotificationEventBus eventBus) {
        this.rateLimiter = rateLimiter;
        this.eventBus = eventBus;
    }

    @Override
    public SendResult handle(SendContext context, SendHandler next) {
        if (rateLimiter == null) {
            return next.handle(context, null);
        }

        Priority priority = context.getSpec().getPriority();
        if (priority == Priority.URGENT) {
            log.debug("Bypassing rate limit for URGENT notification on channel '{}'",
                    context.getChannelName());
            return next.handle(context, null);
        }

        if (!rateLimiter.tryAcquire(context.getChannelName())) {
            log.warn("Rate limit exceeded for channel '{}'", context.getChannelName());
            if (eventBus != null) {
                eventBus.publish(NotificationEvent.builder(EventType.RATE_LIMITED, context.getChannelName())
                        .build());
            }
            throw new RateLimitExceededException(context.getChannelName());
        }

        return next.handle(context, null);
    }
}
