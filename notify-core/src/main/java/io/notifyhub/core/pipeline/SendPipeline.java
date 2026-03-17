package io.notifyhub.core.pipeline;

import io.notifyhub.core.channel.SendResult;
import io.notifyhub.core.dedup.DeduplicationStore;
import io.notifyhub.core.dlq.DeadLetterQueue;
import io.notifyhub.core.event.NotificationEventBus;
import io.notifyhub.core.ratelimit.RateLimiter;
import io.notifyhub.core.template.TemplateEngine;

/**
 * Assembles and executes the resilience handler chain for sending notifications.
 *
 * <p>The pipeline order is:</p>
 * <pre>
 *   DeduplicationHandler → RateLimitHandler → TemplateHandler → RetrySendHandler
 * </pre>
 *
 * <p>Each handler delegates to the next via {@link SendHandler#handle(SendContext, SendHandler)}.
 * {@link RetrySendHandler} is the terminal handler — it does not call {@code next}.</p>
 *
 * <p>Use the static factory {@link #build} to construct a pipeline from optional dependencies.</p>
 *
 * <pre>{@code
 * SendPipeline pipeline = SendPipeline.build(deduplicationStore, rateLimiter, templateEngine, dlq, eventBus);
 * SendResult result = pipeline.execute(context);
 * }</pre>
 */
public class SendPipeline {

    private final SendHandler head;

    /**
     * Create a pipeline with a pre-built handler chain head.
     *
     * @param head the first handler in the chain
     */
    SendPipeline(SendHandler head) {
        this.head = head;
    }

    /**
     * Build a standard pipeline with the given optional dependencies.
     *
     * @param deduplicationStore dedup store, or {@code null} to skip dedup
     * @param rateLimiter        rate limiter, or {@code null} to skip rate limiting
     * @param templateEngine     template engine, or {@code null} to skip template rendering
     * @param deadLetterQueue    DLQ, or {@code null} to skip dead-letter enqueue
     * @param eventBus           event bus, or {@code null} to skip event publishing
     * @return a fully assembled {@link SendPipeline}
     */
    public static SendPipeline build(
            DeduplicationStore deduplicationStore,
            RateLimiter rateLimiter,
            TemplateEngine templateEngine,
            DeadLetterQueue deadLetterQueue,
            NotificationEventBus eventBus) {

        RetrySendHandler terminal = new RetrySendHandler(deadLetterQueue, eventBus);
        TemplateHandler templateHandler = new TemplateHandler(templateEngine);
        RateLimitHandler rateLimitHandler = new RateLimitHandler(rateLimiter, eventBus);
        DeduplicationHandler deduplicationHandler = new DeduplicationHandler(deduplicationStore, eventBus);

        // Link the chain: dedup → rateLimit → template → terminal
        // Each handler calls next.handle(context, null) — only the terminal receives null as next.
        // We wrap each step so the "next" pointer is correctly forwarded.

        SendHandler chain = buildChain(deduplicationHandler, rateLimitHandler, templateHandler, terminal);
        return new SendPipeline(chain);
    }

    /**
     * Execute the pipeline for the given context.
     *
     * @param context the send context
     * @return the result, or {@code null} if skipped (e.g., deduplication)
     */
    public SendResult execute(SendContext context) {
        return head.handle(context, null);
    }

    /**
     * Links the four handlers into an explicit chain.
     *
     * <p>Each intermediate handler ignores the {@code next} arg it receives and instead
     * calls its hard-wired successor. This matches the contract in the handler spec where
     * each handler calls {@code next.handle(context, null)}.</p>
     */
    private static SendHandler buildChain(
            DeduplicationHandler dedup,
            RateLimitHandler rateLimit,
            TemplateHandler template,
            RetrySendHandler terminal) {

        // terminal is its own handler — wrap each preceding step
        SendHandler step3 = (ctx, ignored) -> template.handle(ctx, (c, x) -> terminal.handle(c, null));
        SendHandler step2 = (ctx, ignored) -> rateLimit.handle(ctx, step3);
        SendHandler step1 = (ctx, ignored) -> dedup.handle(ctx, step2);

        return step1;
    }
}
