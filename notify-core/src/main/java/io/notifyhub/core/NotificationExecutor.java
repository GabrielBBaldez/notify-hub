package io.notifyhub.core;

import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
import io.notifyhub.core.channel.SendResult;
import io.notifyhub.core.dedup.DeduplicationStore;
import io.notifyhub.core.dlq.DeadLetter;
import io.notifyhub.core.dlq.DeadLetterQueue;
import io.notifyhub.core.event.EventType;
import io.notifyhub.core.event.NotificationEvent;
import io.notifyhub.core.event.NotificationEventBus;
import io.notifyhub.core.ratelimit.RateLimitExceededException;
import io.notifyhub.core.ratelimit.RateLimiter;
import io.notifyhub.core.retry.RetryPolicy;
import io.notifyhub.core.template.TemplateEngine;
import io.notifyhub.core.template.VersionedTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * Executes notification delivery through channels with retry, rate limiting,
 * deduplication, dead letter queue, and event publishing support.
 *
 * <p>This class is package-private and intended to be used internally by
 * {@link NotifyHub}. It encapsulates all execution logic that was previously
 * embedded in the hub facade.</p>
 */
class NotificationExecutor {

    private static final Logger log = LoggerFactory.getLogger(NotificationExecutor.class);

    private final Map<String, NotificationChannel> channels;
    private final TemplateEngine templateEngine;
    private final RetryPolicy defaultRetryPolicy;
    private final NotificationEventBus eventBus;
    private final DeduplicationStore deduplicationStore;
    private final RateLimiter rateLimiter;
    private final DeadLetterQueue deadLetterQueue;
    private final NotificationTracker tracker;
    private final ExecutorService asyncExecutor;

    NotificationExecutor(
            Map<String, NotificationChannel> channels,
            TemplateEngine templateEngine,
            RetryPolicy defaultRetryPolicy,
            NotificationEventBus eventBus,
            DeduplicationStore deduplicationStore,
            RateLimiter rateLimiter,
            DeadLetterQueue deadLetterQueue,
            NotificationTracker tracker,
            ExecutorService asyncExecutor
    ) {
        this.channels = Objects.requireNonNull(channels, "channels must not be null");
        this.templateEngine = templateEngine;
        this.defaultRetryPolicy = defaultRetryPolicy != null ? defaultRetryPolicy : RetryPolicy.none();
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus must not be null");
        this.deduplicationStore = deduplicationStore;
        this.rateLimiter = rateLimiter;
        this.deadLetterQueue = deadLetterQueue;
        this.tracker = tracker;
        this.asyncExecutor = asyncExecutor;
    }

    // ===================== EXECUTION =====================

    /**
     * Execute notification: send through first channel, try fallbacks on failure.
     */
    void execute(NotificationBuilder builder) {
        List<String> allChannels = new ArrayList<>(builder.getChannels());
        allChannels.addAll(builder.getFallbackChannels());

        NotificationSendException lastException = null;

        for (String channelName : allChannels) {
            try {
                sendToChannel(channelName, builder);
                return; // success -- stop trying
            } catch (NotificationSendException e) {
                lastException = e;
                log.warn("Channel '{}' failed: {}. Trying next fallback...",
                        channelName, e.getMessage());
                publishFailedEvent(channelName, builder, e);
            }
        }

        // All channels failed
        if (lastException != null) {
            log.error("All channels failed for notification. Template: '{}', Recipient channels tried: {}",
                    builder.getTemplateName(), allChannels);
            throw lastException;
        }
    }

    /**
     * Execute notification on ALL channels simultaneously.
     */
    void executeAll(NotificationBuilder builder) {
        List<String> channelNames = builder.getChannels();
        List<NotificationSendException> failures = new ArrayList<>();

        for (String channelName : channelNames) {
            try {
                sendToChannel(channelName, builder);
            } catch (NotificationSendException e) {
                failures.add(e);
                log.warn("Channel '{}' failed during sendAll: {}", channelName, e.getMessage());
                publishFailedEvent(channelName, builder, e);
            }
        }

        if (failures.size() == channelNames.size()) {
            throw new NotificationSendException("all",
                    "All " + failures.size() + " channels failed");
        }
    }

    /**
     * Async version of execute(). Returns CompletableFuture.
     */
    CompletableFuture<Void> executeAsync(NotificationBuilder builder) {
        return CompletableFuture.runAsync(() -> execute(builder), getExecutor());
    }

    /**
     * Async version of executeAll(). Returns CompletableFuture.
     */
    CompletableFuture<Void> executeAllAsync(NotificationBuilder builder) {
        return CompletableFuture.runAsync(() -> executeAll(builder), getExecutor());
    }

    // ===================== TRACKED EXECUTION =====================

    /**
     * Execute and return a delivery receipt for each channel.
     */
    DeliveryReceipt executeTracked(NotificationBuilder builder) {
        List<String> allChannels = new ArrayList<>(builder.getChannels());
        allChannels.addAll(builder.getFallbackChannels());

        NotificationSendException lastException = null;
        String lastChannelName = allChannels.isEmpty() ? "unknown" : allChannels.get(0);
        String recipient = null;

        for (String channelName : allChannels) {
            lastChannelName = channelName;
            try {
                recipient = builder.resolveRecipient(channelName);
                SendResult result = sendToChannel(channelName, builder);

                // Success -- create receipt with optional provider message ID
                DeliveryReceipt receipt = DeliveryReceipt.builder()
                        .channelName(channelName)
                        .recipient(recipient)
                        .status(DeliveryStatus.SENT)
                        .templateName(builder.getTemplateName())
                        .providerMessageId(result != null ? result.getProviderMessageId() : null)
                        .build();

                if (tracker != null) {
                    tracker.record(receipt);
                }
                return receipt;
            } catch (NotificationSendException e) {
                lastException = e;
                log.warn("Channel '{}' failed: {}. Trying next fallback...",
                        channelName, e.getMessage());
                publishFailedEvent(channelName, builder, e);
            }
        }

        // All failed -- create failed receipt
        DeliveryReceipt failedReceipt = DeliveryReceipt.builder()
                .channelName(lastChannelName)
                .recipient(recipient)
                .status(DeliveryStatus.FAILED)
                .templateName(builder.getTemplateName())
                .errorMessage(lastException != null ? lastException.getMessage() : "Unknown error")
                .build();

        if (tracker != null) {
            tracker.record(failedReceipt);
        }

        if (lastException != null) {
            throw lastException;
        }
        throw new NotificationSendException("unknown", "All channels failed to send notification");
    }

    /**
     * Execute tracked on ALL channels. Returns list of receipts.
     */
    List<DeliveryReceipt> executeAllTracked(NotificationBuilder builder) {
        List<String> channelNames = builder.getChannels();
        List<DeliveryReceipt> receipts = new ArrayList<>();
        List<NotificationSendException> failures = new ArrayList<>();

        for (String channelName : channelNames) {
            String recipient = builder.resolveRecipient(channelName);
            try {
                SendResult result = sendToChannel(channelName, builder);

                DeliveryReceipt receipt = DeliveryReceipt.builder()
                        .channelName(channelName)
                        .recipient(recipient)
                        .status(DeliveryStatus.SENT)
                        .templateName(builder.getTemplateName())
                        .providerMessageId(result != null ? result.getProviderMessageId() : null)
                        .build();

                receipts.add(receipt);
                if (tracker != null) {
                    tracker.record(receipt);
                }
            } catch (NotificationSendException e) {
                failures.add(e);
                log.warn("Channel '{}' failed during sendAllTracked: {}", channelName, e.getMessage());
                publishFailedEvent(channelName, builder, e);

                DeliveryReceipt failedReceipt = DeliveryReceipt.builder()
                        .channelName(channelName)
                        .recipient(recipient)
                        .status(DeliveryStatus.FAILED)
                        .templateName(builder.getTemplateName())
                        .errorMessage(e.getMessage())
                        .build();

                receipts.add(failedReceipt);
                if (tracker != null) {
                    tracker.record(failedReceipt);
                }
            }
        }

        if (failures.size() == channelNames.size()) {
            throw new NotificationSendException("all",
                    "All " + failures.size() + " channels failed");
        }

        return Collections.unmodifiableList(receipts);
    }

    // ===================== INTERNAL =====================

    private ExecutorService getExecutor() {
        return asyncExecutor != null ? asyncExecutor : ForkJoinPool.commonPool();
    }

    SendResult sendToChannel(String channelName, NotificationBuilder builder) {
        NotificationChannel channel = channels.get(channelName);
        if (channel == null) {
            String suggestion = suggestClosestChannel(channelName);
            String message = "Channel '" + channelName + "' not registered. Available: " + channels.keySet();
            if (suggestion != null) {
                message += ". Did you mean '" + suggestion + "'?";
            }
            throw new NotificationSendException(channelName, message);
        }

        // Deduplication check (before rate limiting and sending)
        if (deduplicationStore != null) {
            String dedupKey = builder.getDeduplicationKey() != null
                    ? builder.getDeduplicationKey()
                    : builder.computeDeduplicationHash();
            if (deduplicationStore.isDuplicate(dedupKey)) {
                log.info("Duplicate notification skipped for channel '{}' (key: {})", channelName, dedupKey);
                return null;
            }
        }

        // Rate limiting (URGENT bypasses)
        if (rateLimiter != null && builder.getPriority() != Priority.URGENT) {
            if (!rateLimiter.tryAcquire(channelName)) {
                throw new RateLimitExceededException(channelName);
            }
        }

        String recipient = builder.resolveRecipient(channelName);
        if (recipient == null || recipient.isBlank()) {
            throw new NotificationSendException(channelName,
                    "No recipient address available for channel '" + channelName + "'");
        }

        // Render template (with optional version support)
        String renderedContent = null;
        if (builder.getTemplateName() != null && templateEngine != null) {
            String variant = channelName.equals("email") ? "html" : "txt";
            if (builder.getTemplateVersion() != null && templateEngine instanceof VersionedTemplateEngine vte) {
                renderedContent = vte.render(
                        builder.getTemplateName(), builder.getTemplateVersion(),
                        variant, builder.getParams(), builder.getLocale());
            } else {
                renderedContent = templateEngine.render(
                        builder.getTemplateName(), variant, builder.getParams(), builder.getLocale());
            }
        }

        // Build notification object
        Notification notification = new Notification(
                recipient, channelName, builder.getSubject(),
                builder.getTemplateName(), builder.getRawContent(),
                builder.getParams(), builder.getAttachments(),
                builder.getPriority(), builder.getImageUrl()
        );
        if (renderedContent != null) {
            notification = notification.withRenderedContent(renderedContent);
        }

        // Send with retry
        RetryPolicy policy = builder.getRetryPolicy() != null
                ? builder.getRetryPolicy()
                : defaultRetryPolicy;

        SendResult result = sendWithRetry(channel, notification, policy);

        // Mark as sent in dedup store after successful delivery
        if (deduplicationStore != null) {
            String dedupKey = builder.getDeduplicationKey() != null
                    ? builder.getDeduplicationKey()
                    : builder.computeDeduplicationHash();
            deduplicationStore.markSent(dedupKey);
        }

        log.info("Notification sent via '{}' to '{}'", channelName, recipient);
        publishSentEvent(channelName, builder);
        return result;
    }

    SendResult sendWithRetry(NotificationChannel channel, Notification notification,
                             RetryPolicy policy) {
        int attempts = policy.getMaxAttempts();
        NotificationSendException lastException = null;

        for (int i = 0; i < attempts; i++) {
            try {
                if (i > 0) {
                    long delayMs = policy.getDelayForAttempt(i - 1).toMillis();
                    log.debug("Retry attempt {}/{} for channel '{}' after {}ms",
                            i + 1, attempts, channel.getName(), delayMs);
                    Thread.sleep(delayMs);
                }
                return channel.sendWithResult(notification); // success -- return provider metadata
            } catch (NotificationSendException e) {
                lastException = e;
                log.warn("Attempt {}/{} failed for channel '{}': {}",
                        i + 1, attempts, channel.getName(), e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new NotificationSendException(channel.getName(),
                        "Retry interrupted", e);
            } catch (Exception e) {
                lastException = new NotificationSendException(channel.getName(),
                        "Unexpected error: " + e.getMessage(), e);
            }
        }

        // All retries exhausted -- enqueue to DLQ if configured
        if (deadLetterQueue != null) {
            DeadLetter dl = DeadLetter.builder()
                    .channelName(channel.getName())
                    .recipient(notification.getRecipient())
                    .subject(notification.getSubject())
                    .content(notification.getRenderedContent())
                    .templateName(notification.getTemplateName())
                    .errorMessage(lastException != null ? lastException.getMessage() : "Unknown error")
                    .attemptCount(attempts)
                    .build();
            deadLetterQueue.enqueue(dl);
            log.warn("Notification moved to DLQ after {} failed attempts for channel '{}'",
                    attempts, channel.getName());
        }

        if (lastException != null) {
            throw lastException;
        }
        throw new NotificationSendException(channel.getName(), "All retry attempts failed");
    }

    // ===================== CHANNEL SUGGESTION =====================

    private String suggestClosestChannel(String input) {
        String closest = null;
        int minDistance = Integer.MAX_VALUE;
        for (String name : channels.keySet()) {
            int distance = levenshteinDistance(input, name);
            if (distance < minDistance && distance <= 2) {
                minDistance = distance;
                closest = name;
            }
        }
        return closest;
    }

    private static int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }

    // ===================== ACCESSORS =====================

    NotificationTracker getTracker() {
        return tracker;
    }

    DeadLetterQueue getDeadLetterQueue() {
        return deadLetterQueue;
    }

    // ===================== EVENT PUBLISHING =====================

    private void publishFailedEvent(String channelName, NotificationBuilder builder, Exception error) {
        eventBus.publish(
                NotificationEvent.builder(EventType.FAILED, channelName)
                        .templateName(builder.getTemplateName())
                        .errorMessage(error.getMessage())
                        .build()
        );
    }

    private void publishSentEvent(String channelName, NotificationBuilder builder) {
        eventBus.publish(
                NotificationEvent.builder(EventType.SENT, channelName)
                        .templateName(builder.getTemplateName())
                        .build()
        );
    }
}
