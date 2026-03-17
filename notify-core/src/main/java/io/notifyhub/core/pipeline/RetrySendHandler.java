package io.notifyhub.core.pipeline;

import io.notifyhub.core.channel.NotificationSendException;
import io.notifyhub.core.channel.SendResult;
import io.notifyhub.core.dlq.DeadLetter;
import io.notifyhub.core.dlq.DeadLetterQueue;
import io.notifyhub.core.event.EventType;
import io.notifyhub.core.event.NotificationEvent;
import io.notifyhub.core.event.NotificationEventBus;
import io.notifyhub.core.retry.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Terminal pipeline handler that performs the actual channel send with retry support.
 *
 * <p>This handler does NOT call {@code next.handle()} — it is the end of the chain.
 * It wraps the channel send with the configured {@link RetryPolicy}:</p>
 * <ul>
 *   <li>On each retry, publishes a {@code RETRIED} event.</li>
 *   <li>On final failure, publishes a {@code FAILED} event and enqueues to the DLQ.</li>
 * </ul>
 */
class RetrySendHandler implements SendHandler {

    private static final Logger log = LoggerFactory.getLogger(RetrySendHandler.class);

    private final DeadLetterQueue deadLetterQueue;
    private final NotificationEventBus eventBus;

    RetrySendHandler(DeadLetterQueue deadLetterQueue, NotificationEventBus eventBus) {
        this.deadLetterQueue = deadLetterQueue;
        this.eventBus = eventBus;
    }

    @Override
    public SendResult handle(SendContext context, SendHandler next) {
        // next is null — this is the terminal handler
        RetryPolicy policy = context.getRetryPolicy() != null
                ? context.getRetryPolicy()
                : RetryPolicy.none();

        int maxAttempts = policy.getMaxAttempts();
        String channelName = context.getChannelName();
        Exception lastException = null;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            if (attempt > 0) {
                Duration delay = policy.getDelayForAttempt(attempt - 1);
                waitFor(delay);

                log.debug("Retrying send on channel '{}', attempt {}/{}", channelName, attempt + 1, maxAttempts);
                if (eventBus != null) {
                    eventBus.publish(NotificationEvent.builder(EventType.RETRIED, channelName)
                            .metadata(java.util.Map.of(
                                    "attempt", String.valueOf(attempt + 1),
                                    "maxAttempts", String.valueOf(maxAttempts)
                            ))
                            .build());
                }
            }

            try {
                SendResult result = context.getChannel().sendWithResult(context.getNotification());
                log.debug("Notification sent successfully on channel '{}' (attempt {})", channelName, attempt + 1);
                return result;
            } catch (Exception e) {
                lastException = e;
                log.warn("Send attempt {}/{} failed for channel '{}': {}",
                        attempt + 1, maxAttempts, channelName, e.getMessage());
            }
        }

        // All retries exhausted
        log.error("All {} attempt(s) failed for channel '{}'", maxAttempts, channelName);

        if (eventBus != null) {
            eventBus.publish(NotificationEvent.builder(EventType.FAILED, channelName)
                    .errorMessage(lastException != null ? lastException.getMessage() : "Unknown error")
                    .build());
        }

        enqueueDeadLetter(context, maxAttempts, lastException);

        String msg = lastException != null ? lastException.getMessage() : "Unknown error";
        throw new NotificationSendException(channelName,
                "Failed after " + maxAttempts + " attempt(s): " + msg, lastException);
    }

    private void enqueueDeadLetter(SendContext context, int attemptCount, Exception cause) {
        if (deadLetterQueue == null) {
            return;
        }
        var n = context.getNotification();
        if (n == null) {
            return;
        }
        DeadLetter dl = DeadLetter.builder()
                .channelName(context.getChannelName())
                .recipient(n.getRecipient())
                .subject(n.getSubject())
                .content(n.getRenderedContent())
                .templateName(n.getTemplateName())
                .errorMessage(cause != null ? cause.getMessage() : "Unknown error")
                .attemptCount(attemptCount)
                .build();
        deadLetterQueue.enqueue(dl);
        log.debug("Dead letter enqueued for channel '{}', recipient='{}'",
                context.getChannelName(), n.getRecipient());
    }

    private void waitFor(Duration delay) {
        if (delay == null || delay.isZero() || delay.isNegative()) {
            return;
        }
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
