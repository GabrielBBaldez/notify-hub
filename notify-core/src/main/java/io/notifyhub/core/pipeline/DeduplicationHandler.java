package io.notifyhub.core.pipeline;

import io.notifyhub.core.channel.SendResult;
import io.notifyhub.core.dedup.DeduplicationStore;
import io.notifyhub.core.event.EventType;
import io.notifyhub.core.event.NotificationEvent;
import io.notifyhub.core.event.NotificationEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pipeline handler that skips duplicate notifications.
 *
 * <p>If no {@link DeduplicationStore} is configured, this handler is a pass-through.
 * Otherwise it checks whether the notification key was already sent, and if so,
 * logs and returns {@code null} without calling the rest of the pipeline.</p>
 */
class DeduplicationHandler implements SendHandler {

    private static final Logger log = LoggerFactory.getLogger(DeduplicationHandler.class);

    private final DeduplicationStore deduplicationStore;
    private final NotificationEventBus eventBus;

    DeduplicationHandler(DeduplicationStore deduplicationStore, NotificationEventBus eventBus) {
        this.deduplicationStore = deduplicationStore;
        this.eventBus = eventBus;
    }

    @Override
    public SendResult handle(SendContext context, SendHandler next) {
        if (deduplicationStore == null) {
            return next.handle(context, null);
        }

        String key = resolveKey(context);

        if (deduplicationStore.isDuplicate(key)) {
            log.debug("Duplicate notification skipped for channel '{}', key='{}'",
                    context.getChannelName(), key);
            if (eventBus != null) {
                eventBus.publish(NotificationEvent.builder(EventType.DEDUPED, context.getChannelName())
                        .metadata(java.util.Map.of("deduplicationKey", key))
                        .build());
            }
            return null;
        }

        SendResult result = next.handle(context, null);

        // Mark as sent after successful pipeline execution
        deduplicationStore.markSent(key);

        return result;
    }

    private String resolveKey(SendContext context) {
        String explicitKey = context.getSpec().getDeduplicationKey();
        if (explicitKey != null && !explicitKey.isBlank()) {
            return explicitKey;
        }
        return context.getSpec().getDeduplicationHash();
    }
}
