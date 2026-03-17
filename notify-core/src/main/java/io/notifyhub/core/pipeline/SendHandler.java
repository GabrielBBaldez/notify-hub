package io.notifyhub.core.pipeline;

import io.notifyhub.core.channel.SendResult;

/**
 * Handler in the send pipeline chain.
 *
 * <p>Implementations process the {@link SendContext} and call {@code next.handle()} to
 * continue the chain, or short-circuit by returning directly (e.g., deduplication skip).</p>
 *
 * <p>The terminal handler ({@link RetrySendHandler}) does not call next and receives
 * {@code null} as {@code next}.</p>
 */
public interface SendHandler {

    /**
     * Process the send context and optionally delegate to the next handler.
     *
     * @param context the send context holding all notification data
     * @param next    the next handler in the chain, or {@code null} if this is the terminal handler
     * @return the result of the send, or {@code null} if skipped (e.g., deduplication)
     */
    SendResult handle(SendContext context, SendHandler next);
}
