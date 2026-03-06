package io.notifyhub.core;

import io.notifyhub.core.audience.AudienceManager;
import io.notifyhub.core.audience.Contact;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
import io.notifyhub.core.channel.SendResult;
import io.notifyhub.core.dedup.DeduplicationStore;
import io.notifyhub.core.dlq.DeadLetter;
import io.notifyhub.core.dlq.DeadLetterQueue;
import io.notifyhub.core.ratelimit.RateLimitExceededException;
import io.notifyhub.core.ratelimit.RateLimiter;
import io.notifyhub.core.routing.NotificationRouter;
import io.notifyhub.core.retry.RetryPolicy;
import io.notifyhub.core.template.TemplateEngine;
import io.notifyhub.core.template.VersionedTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Main entry point for sending notifications.
 *
 * <pre>{@code
 * NotifyHub notify = NotifyHub.builder()
 *     .templateEngine(new MustacheTemplateEngine())
 *     .channel(new SmtpEmailChannel(config))
 *     .build();
 *
 * // Simple send
 * notify.to(user)
 *     .via(Channel.EMAIL)
 *     .subject("Welcome!")
 *     .template("welcome")
 *     .param("name", user.getName())
 *     .send();
 *
 * // Tracked send (returns receipt)
 * DeliveryReceipt receipt = notify.to(user)
 *     .via(Channel.EMAIL)
 *     .content("Hello!")
 *     .sendTracked();
 *
 * // Scheduled send
 * ScheduledNotification scheduled = notify.to(user)
 *     .via(Channel.EMAIL)
 *     .content("Reminder!")
 *     .schedule(Duration.ofMinutes(30));
 *
 * scheduled.cancel(); // cancel if needed
 * }</pre>
 */
public class NotifyHub {

    private static final Logger log = LoggerFactory.getLogger(NotifyHub.class);

    private final Map<String, NotificationChannel> channels;
    private final TemplateEngine templateEngine;
    private final RetryPolicy defaultRetryPolicy;
    private final List<NotificationListener> listeners;
    private final ExecutorService executor;
    private final ScheduledExecutorService scheduler;
    private final NotificationTracker tracker;
    private final RateLimiter rateLimiter;
    private final DeadLetterQueue deadLetterQueue;
    private final NotificationRouter router;
    private final DeduplicationStore deduplicationStore;
    private final AuditLog auditLog;
    private final AudienceManager audienceManager;

    private NotifyHub(Builder builder) {
        this.channels = new ConcurrentHashMap<>(builder.channels);
        this.templateEngine = builder.templateEngine;
        this.defaultRetryPolicy = builder.defaultRetryPolicy != null
                ? builder.defaultRetryPolicy
                : RetryPolicy.none();
        this.listeners = new ArrayList<>(builder.listeners);
        this.executor = builder.executor;
        this.scheduler = builder.scheduler;
        this.tracker = builder.tracker;
        this.rateLimiter = builder.rateLimiter;
        this.deadLetterQueue = builder.deadLetterQueue;
        this.router = builder.router;
        this.deduplicationStore = builder.deduplicationStore;
        this.auditLog = builder.auditLog;
        this.audienceManager = builder.audienceManager;
    }

    // ===================== FLUENT API ENTRY POINTS =====================

    /**
     * Start building a notification to a Notifiable recipient.
     *
     * <pre>{@code
     * notify.to(user).via(EMAIL).template("welcome").send();
     * }</pre>
     */
    public NotificationBuilder to(Notifiable notifiable) {
        return new NotificationBuilder(this).to(notifiable);
    }

    /**
     * Start building a notification to an email address.
     *
     * <pre>{@code
     * notify.to("user@example.com").via(EMAIL).template("welcome").send();
     * }</pre>
     */
    public NotificationBuilder to(String email) {
        return new NotificationBuilder(this).to(email);
    }

    /**
     * Start building a notification to a phone number.
     *
     * <pre>{@code
     * notify.toPhone("+5548999999999").via(SMS).content("Your code: 1234").send();
     * }</pre>
     */
    public NotificationBuilder toPhone(String phone) {
        return new NotificationBuilder(this).toPhone(phone);
    }

    /**
     * Start building a batch notification to multiple email recipients.
     *
     * <pre>{@code
     * notify.toAll(List.of("a@test.com", "b@test.com"))
     *     .via(Channel.EMAIL)
     *     .template("announcement")
     *     .send();
     * }</pre>
     */
    public BatchNotificationBuilder toAll(java.util.List<String> recipients) {
        return new BatchNotificationBuilder(this, recipients, null);
    }

    /**
     * Start building a batch notification to multiple Notifiable recipients.
     *
     * <pre>{@code
     * notify.toAll(users)
     *     .via(Channel.EMAIL)
     *     .template("announcement")
     *     .send();
     * }</pre>
     */
    @SuppressWarnings("unchecked")
    public <T extends Notifiable> BatchNotificationBuilder toAllNotifiable(java.util.List<T> notifiables) {
        return new BatchNotificationBuilder(this, null, (java.util.List<Notifiable>) (java.util.List<?>) notifiables);
    }

    /**
     * Start building a batch notification to an audience.
     * The audience is resolved to contacts matching all its tags,
     * then each contact receives the notification.
     *
     * <pre>{@code
     * notify.toAudience("premium-users")
     *     .via(Channel.EMAIL)
     *     .template("promo")
     *     .send();
     * }</pre>
     *
     * @param audienceName the name of a previously created audience
     * @return a {@link BatchNotificationBuilder} for the matched contacts
     * @throws IllegalStateException if AudienceManager is not configured
     * @throws IllegalArgumentException if the audience is not found
     */
    @SuppressWarnings("unchecked")
    public BatchNotificationBuilder toAudience(String audienceName) {
        if (audienceManager == null) {
            throw new IllegalStateException(
                    "AudienceManager not configured. Use NotifyHub.builder().audienceManager(mgr).build()");
        }
        List<Contact> contacts = audienceManager.resolve(audienceName);
        return new BatchNotificationBuilder(this, null,
                (java.util.List<Notifiable>) (java.util.List<?>) contacts);
    }

    // ===================== CHANNEL MANAGEMENT =====================

    /** Register a new channel at runtime. */
    public void registerChannel(NotificationChannel channel) {
        channels.put(channel.getName().toLowerCase(), channel);
        log.info("Registered notification channel: {}", channel.getName());
    }

    /** Get a registered channel by name. */
    public Optional<NotificationChannel> getChannel(String name) {
        return Optional.ofNullable(channels.get(name.toLowerCase()));
    }

    /** Get all registered channel names. */
    public Set<String> getRegisteredChannels() {
        return Collections.unmodifiableSet(channels.keySet());
    }

    /** Get the notification tracker (if configured). */
    public NotificationTracker getTracker() {
        return tracker;
    }

    /** Get the dead letter queue (if configured). */
    public DeadLetterQueue getDeadLetterQueue() {
        return deadLetterQueue;
    }

    /** Get the audit log, or null if not configured. */
    public AuditLog getAuditLog() {
        return auditLog;
    }

    /** Get the audience manager, or null if not configured. */
    public AudienceManager getAudienceManager() {
        return audienceManager;
    }

    /**
     * Send to a Notifiable, auto-routing through their preferred channels.
     * The first preferred channel is the primary, the rest are fallbacks.
     * If no preferred channels, requires .via() to be set manually.
     *
     * <pre>{@code
     * notify.notify(user)
     *     .subject("Welcome!")
     *     .template("welcome")
     *     .send();
     * }</pre>
     */
    public NotificationBuilder notify(Notifiable notifiable) {
        NotificationBuilder builder = new NotificationBuilder(this).to(notifiable);
        List<Channel> preferred = notifiable.getPreferredChannels();
        if (!preferred.isEmpty()) {
            builder.via(preferred.get(0));
            for (int i = 1; i < preferred.size(); i++) {
                builder.fallback(preferred.get(i));
            }
        }
        return builder;
    }

    // ===================== EXECUTION =====================

    /**
     * Execute notification: send through first channel, try fallbacks on failure.
     * Called internally by NotificationBuilder.send().
     */
    void execute(NotificationBuilder builder) {
        List<String> allChannels = new ArrayList<>(builder.getChannels());
        allChannels.addAll(builder.getFallbackChannels());

        NotificationSendException lastException = null;

        for (String channelName : allChannels) {
            try {
                sendToChannel(channelName, builder);
                return; // success — stop trying
            } catch (NotificationSendException e) {
                lastException = e;
                log.warn("Channel '{}' failed: {}. Trying next fallback...",
                        channelName, e.getMessage());
                notifyListeners(channelName, builder, e);
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
     * Called internally by NotificationBuilder.sendAll().
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
                notifyListeners(channelName, builder, e);
            }
        }

        if (failures.size() == channelNames.size()) {
            throw new NotificationSendException("all",
                    "All " + failures.size() + " channels failed");
        }
    }

    /**
     * Async version of execute(). Returns CompletableFuture.
     * Called internally by NotificationBuilder.sendAsync().
     */
    CompletableFuture<Void> executeAsync(NotificationBuilder builder) {
        return CompletableFuture.runAsync(() -> execute(builder), getExecutor());
    }

    /**
     * Async version of executeAll(). Returns CompletableFuture.
     * Called internally by NotificationBuilder.sendAllAsync().
     */
    CompletableFuture<Void> executeAllAsync(NotificationBuilder builder) {
        return CompletableFuture.runAsync(() -> executeAll(builder), getExecutor());
    }

    // ===================== TRACKED EXECUTION =====================

    /**
     * Execute and return a delivery receipt for each channel.
     * Called internally by NotificationBuilder.sendTracked().
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

                // Success — create receipt with optional provider message ID
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
                notifyListeners(channelName, builder, e);
            }
        }

        // All failed — create failed receipt
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

        throw lastException;
    }

    /**
     * Execute tracked on ALL channels. Returns list of receipts.
     * Called internally by NotificationBuilder.sendAllTracked().
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
                notifyListeners(channelName, builder, e);

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

    // ===================== SCHEDULED EXECUTION =====================

    /**
     * Schedule a notification for future delivery.
     * Called internally by NotificationBuilder.schedule().
     */
    ScheduledNotification executeScheduled(NotificationBuilder builder, Duration delay) {
        ScheduledExecutorService sched = getScheduler();
        String primaryChannel = builder.getChannels().get(0);
        String recipient = builder.resolveRecipient(primaryChannel);
        Instant scheduledTime = Instant.now().plus(delay);

        // Use AtomicReference to share the ScheduledNotification between caller and scheduled task
        var ref = new java.util.concurrent.atomic.AtomicReference<ScheduledNotification>();

        // Schedule the actual execution
        ScheduledFuture<?> future = sched.schedule(() -> {
            ScheduledNotification sn = ref.get();
            try {
                execute(builder);
                if (sn != null) sn.markSent();

                // Update tracker with SENT status
                if (tracker != null && sn != null) {
                    DeliveryReceipt sentReceipt = DeliveryReceipt.builder()
                            .id(sn.getId() + "-sent")
                            .channelName(primaryChannel)
                            .recipient(recipient)
                            .status(DeliveryStatus.SENT)
                            .templateName(builder.getTemplateName())
                            .build();
                    tracker.record(sentReceipt);
                }
            } catch (Exception e) {
                if (sn != null) sn.markFailed();
                log.error("Scheduled notification failed: {}", e.getMessage());

                // Update tracker with FAILED status
                if (tracker != null && sn != null) {
                    DeliveryReceipt failedReceipt = DeliveryReceipt.builder()
                            .id(sn.getId() + "-failed")
                            .channelName(primaryChannel)
                            .recipient(recipient)
                            .status(DeliveryStatus.FAILED)
                            .templateName(builder.getTemplateName())
                            .errorMessage(e.getMessage())
                            .build();
                    tracker.record(failedReceipt);
                }
            }
        }, delay.toMillis(), TimeUnit.MILLISECONDS);

        // Create the ScheduledNotification with the actual future and share it
        ScheduledNotification scheduled = new ScheduledNotification(
                future, scheduledTime, primaryChannel, recipient
        );
        ref.set(scheduled);

        // Record as SCHEDULED in tracker
        if (tracker != null) {
            DeliveryReceipt scheduledReceipt = DeliveryReceipt.builder()
                    .id(scheduled.getId())
                    .channelName(primaryChannel)
                    .recipient(recipient)
                    .status(DeliveryStatus.SCHEDULED)
                    .templateName(builder.getTemplateName())
                    .build();
            tracker.record(scheduledReceipt);
        }

        // Wire cancel callback to notify listeners
        scheduled.setCancelCallback(() -> {
            for (NotificationListener listener : listeners) {
                try {
                    listener.onCancelled(primaryChannel, recipient);
                } catch (Exception e) {
                    log.warn("Listener error on cancelled: {}", e.getMessage());
                }
            }
        });

        // Notify listeners
        for (NotificationListener listener : listeners) {
            try {
                listener.onScheduled(primaryChannel, recipient, delay);
            } catch (Exception e) {
                log.warn("Listener error on scheduled: {}", e.getMessage());
            }
        }

        return scheduled;
    }

    // ===================== INTERNAL =====================

    private ExecutorService getExecutor() {
        return executor != null ? executor : ForkJoinPool.commonPool();
    }

    private ScheduledExecutorService getScheduler() {
        if (scheduler != null) {
            return scheduler;
        }
        // Default: create a single-threaded scheduler
        return Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "notifyhub-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    private SendResult sendToChannel(String channelName, NotificationBuilder builder) {
        NotificationChannel channel = channels.get(channelName);
        if (channel == null) {
            throw new NotificationSendException(channelName,
                    "Channel '" + channelName + "' not registered. Available: " + channels.keySet());
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
            notification.setRenderedContent(renderedContent);
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
        notifyListenersSuccess(channelName, builder);
        return result;
    }

    private SendResult sendWithRetry(NotificationChannel channel, Notification notification,
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
                return channel.sendWithResult(notification); // success — return provider metadata
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

        // All retries exhausted — enqueue to DLQ if configured
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

        throw lastException;
    }

    // ===================== LISTENERS =====================

    private void notifyListeners(String channelName, NotificationBuilder builder, Exception error) {
        for (NotificationListener listener : listeners) {
            try {
                listener.onFailure(channelName, builder.getTemplateName(), error);
            } catch (Exception e) {
                log.warn("Listener error: {}", e.getMessage());
            }
        }
    }

    private void notifyListenersSuccess(String channelName, NotificationBuilder builder) {
        for (NotificationListener listener : listeners) {
            try {
                listener.onSuccess(channelName, builder.getTemplateName());
            } catch (Exception e) {
                log.warn("Listener error: {}", e.getMessage());
            }
        }
    }

    // ===================== BUILDER =====================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<String, NotificationChannel> channels = new LinkedHashMap<>();
        private TemplateEngine templateEngine;
        private RetryPolicy defaultRetryPolicy;
        private final List<NotificationListener> listeners = new ArrayList<>();
        private ExecutorService executor;
        private ScheduledExecutorService scheduler;
        private NotificationTracker tracker;
        private RateLimiter rateLimiter;
        private DeadLetterQueue deadLetterQueue;
        private NotificationRouter router;
        private DeduplicationStore deduplicationStore;
        private AuditLog auditLog;
        private AudienceManager audienceManager;

        public Builder channel(NotificationChannel channel) {
            this.channels.put(channel.getName().toLowerCase(), channel);
            return this;
        }

        public Builder channels(Collection<? extends NotificationChannel> channels) {
            for (NotificationChannel ch : channels) {
                this.channels.put(ch.getName().toLowerCase(), ch);
            }
            return this;
        }

        public Builder templateEngine(TemplateEngine templateEngine) {
            this.templateEngine = templateEngine;
            return this;
        }

        public Builder defaultRetryPolicy(RetryPolicy retryPolicy) {
            this.defaultRetryPolicy = retryPolicy;
            return this;
        }

        public Builder listener(NotificationListener listener) {
            this.listeners.add(listener);
            return this;
        }

        /**
         * Set the executor for async operations (sendAsync, sendAllAsync).
         * If not set, defaults to {@link ForkJoinPool#commonPool()}.
         */
        public Builder executor(ExecutorService executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Set the scheduler for scheduled notifications (schedule, scheduleAt).
         * If not set, a default single-threaded daemon scheduler is created.
         */
        public Builder scheduler(ScheduledExecutorService scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        /**
         * Set the notification tracker for delivery tracking.
         * If not set, delivery receipts are only returned but not persisted.
         *
         * @see InMemoryNotificationTracker
         */
        public Builder tracker(NotificationTracker tracker) {
            this.tracker = tracker;
            return this;
        }

        /**
         * Set the rate limiter for controlling notification throughput.
         * URGENT priority notifications bypass rate limiting.
         */
        public Builder rateLimiter(RateLimiter rateLimiter) {
            this.rateLimiter = rateLimiter;
            return this;
        }

        /**
         * Set the dead letter queue for failed notifications.
         * Notifications that fail after all retries are moved to the DLQ.
         */
        public Builder deadLetterQueue(DeadLetterQueue deadLetterQueue) {
            this.deadLetterQueue = deadLetterQueue;
            return this;
        }

        /**
         * Set the notification router for conditional routing.
         * When configured, notifications can use .route() to auto-select channels.
         */
        public Builder router(NotificationRouter router) {
            this.router = router;
            return this;
        }

        /**
         * Set the deduplication store for preventing duplicate notifications.
         * When configured, identical notifications are silently skipped.
         *
         * @see io.notifyhub.core.dedup.InMemoryDeduplicationStore
         */
        public Builder deduplicationStore(DeduplicationStore deduplicationStore) {
            this.deduplicationStore = deduplicationStore;
            return this;
        }

        /**
         * Set the audit log for recording system events.
         *
         * @see InMemoryAuditLog
         */
        public Builder auditLog(AuditLog auditLog) {
            this.auditLog = auditLog;
            return this;
        }

        /**
         * Set the audience manager for audience segmentation.
         * When configured, enables {@link NotifyHub#toAudience(String)}.
         */
        public Builder audienceManager(AudienceManager audienceManager) {
            this.audienceManager = audienceManager;
            return this;
        }

        public NotifyHub build() {
            return new NotifyHub(this);
        }
    }
}
