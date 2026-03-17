package io.notifyhub.core;

import io.notifyhub.core.audience.AudienceManager;
import io.notifyhub.core.audience.Contact;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.dedup.DeduplicationStore;
import io.notifyhub.core.dlq.DeadLetterQueue;
import io.notifyhub.core.event.LegacyListenerAdapter;
import io.notifyhub.core.event.NotificationEventBus;
import io.notifyhub.core.event.NotificationEventListener;
import io.notifyhub.core.ratelimit.RateLimiter;
import io.notifyhub.core.resilience.BulkheadConfig;
import io.notifyhub.core.resilience.CircuitBreakerConfig;
import io.notifyhub.core.retry.RetryPolicy;
import io.notifyhub.core.routing.NotificationRouter;
import io.notifyhub.core.template.TemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
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
    private final NotificationExecutor executor;
    private final NotificationScheduler scheduler;
    private final NotificationEventBus eventBus;
    private final AudienceManager audienceManager;
    private final AuditLog auditLog;
    private final CircuitBreakerConfig circuitBreakerConfig;
    private final BulkheadConfig bulkheadConfig;

    private NotifyHub(Builder builder) {
        this.channels = new ConcurrentHashMap<>(builder.channels);

        // Build the event bus: new EventBus listeners first, then legacy listener adapters
        List<NotificationEventListener> eventListeners = new ArrayList<>(builder.eventListeners);
        for (NotificationListener listener : builder.listeners) {
            eventListeners.add(new LegacyListenerAdapter(listener));
        }
        this.eventBus = new NotificationEventBus(eventListeners);

        // Resolve default retry policy
        RetryPolicy retryPolicy = builder.defaultRetryPolicy != null
                ? builder.defaultRetryPolicy
                : RetryPolicy.none();

        // Create the executor with all dependencies
        this.executor = new NotificationExecutor(
                this.channels,
                builder.templateEngine,
                retryPolicy,
                this.eventBus,
                builder.deduplicationStore,
                builder.rateLimiter,
                builder.deadLetterQueue,
                builder.tracker,
                builder.executor
        );

        // Create the scheduler with executor and event bus
        this.scheduler = new NotificationScheduler(
                this.executor,
                this.eventBus,
                builder.scheduler,
                builder.tracker
        );

        this.audienceManager = builder.audienceManager;
        this.auditLog = builder.auditLog;
        this.circuitBreakerConfig = builder.circuitBreakerConfig;
        this.bulkheadConfig = builder.bulkheadConfig;
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
        return executor.getTracker();
    }

    /** Get the dead letter queue (if configured). */
    public DeadLetterQueue getDeadLetterQueue() {
        return executor.getDeadLetterQueue();
    }

    /** Get the audit log, or null if not configured. */
    public AuditLog getAuditLog() {
        return auditLog;
    }

    /** Get the audience manager, or null if not configured. */
    public AudienceManager getAudienceManager() {
        return audienceManager;
    }

    /** Get the circuit breaker config, or null if not configured. */
    public CircuitBreakerConfig getCircuitBreakerConfig() {
        return circuitBreakerConfig;
    }

    /** Get the bulkhead config, or null if not configured. */
    public BulkheadConfig getBulkheadConfig() {
        return bulkheadConfig;
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

    // ===================== EXECUTION (delegation to NotificationExecutor) =====================

    /**
     * Execute notification: send through first channel, try fallbacks on failure.
     * Called internally by NotificationBuilder.send().
     */
    void execute(NotificationBuilder builder) {
        executor.execute(builder);
    }

    /**
     * Execute notification on ALL channels simultaneously.
     * Called internally by NotificationBuilder.sendAll().
     */
    void executeAll(NotificationBuilder builder) {
        executor.executeAll(builder);
    }

    /**
     * Async version of execute(). Returns CompletableFuture.
     * Called internally by NotificationBuilder.sendAsync().
     */
    CompletableFuture<Void> executeAsync(NotificationBuilder builder) {
        return executor.executeAsync(builder);
    }

    /**
     * Async version of executeAll(). Returns CompletableFuture.
     * Called internally by NotificationBuilder.sendAllAsync().
     */
    CompletableFuture<Void> executeAllAsync(NotificationBuilder builder) {
        return executor.executeAllAsync(builder);
    }

    // ===================== TRACKED EXECUTION (delegation to NotificationExecutor) =====================

    /**
     * Execute and return a delivery receipt for each channel.
     * Called internally by NotificationBuilder.sendTracked().
     */
    DeliveryReceipt executeTracked(NotificationBuilder builder) {
        return executor.executeTracked(builder);
    }

    /**
     * Execute tracked on ALL channels. Returns list of receipts.
     * Called internally by NotificationBuilder.sendAllTracked().
     */
    List<DeliveryReceipt> executeAllTracked(NotificationBuilder builder) {
        return executor.executeAllTracked(builder);
    }

    // ===================== SCHEDULED EXECUTION (delegation to NotificationScheduler) =====================

    /**
     * Schedule a notification for future delivery.
     * Called internally by NotificationBuilder.schedule().
     */
    ScheduledNotification executeScheduled(NotificationBuilder builder, Duration delay) {
        return scheduler.schedule(builder, delay);
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
        private final List<NotificationEventListener> eventListeners = new ArrayList<>();
        private ExecutorService executor;
        private ScheduledExecutorService scheduler;
        private NotificationTracker tracker;
        private RateLimiter rateLimiter;
        private DeadLetterQueue deadLetterQueue;
        private NotificationRouter router;
        private DeduplicationStore deduplicationStore;
        private AuditLog auditLog;
        private AudienceManager audienceManager;
        private CircuitBreakerConfig circuitBreakerConfig;
        private BulkheadConfig bulkheadConfig;

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
         * Add an EventBus-based event listener.
         * These listeners receive {@link io.notifyhub.core.event.NotificationEvent} objects
         * and are called directly by the event bus (no adapter needed).
         */
        public Builder eventListener(NotificationEventListener listener) {
            this.eventListeners.add(listener);
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

        /**
         * Set the circuit breaker configuration for fault tolerance.
         * When configured, channels are protected by a circuit breaker
         * that opens after a threshold of failures.
         *
         * @see CircuitBreakerConfig
         */
        public Builder circuitBreaker(CircuitBreakerConfig config) {
            this.circuitBreakerConfig = config;
            return this;
        }

        /**
         * Set the bulkhead configuration for per-channel concurrency limiting.
         * When configured, limits the number of concurrent calls per channel
         * to prevent resource exhaustion.
         *
         * @see BulkheadConfig
         */
        public Builder bulkhead(BulkheadConfig config) {
            this.bulkheadConfig = config;
            return this;
        }

        public NotifyHub build() {
            return new NotifyHub(this);
        }
    }
}
