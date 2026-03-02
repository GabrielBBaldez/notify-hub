package io.notifyhub.spring;

import io.notifyhub.channel.email.SmtpConfig;
import io.notifyhub.channel.email.SmtpEmailChannel;
import io.notifyhub.core.AuditLog;
import io.notifyhub.core.AuditNotificationListener;
import io.notifyhub.core.InMemoryAuditLog;
import io.notifyhub.core.NotifyHub;
import io.notifyhub.core.audience.AudienceManager;
import io.notifyhub.core.audience.ContactRepository;
import io.notifyhub.core.audience.InMemoryContactRepository;
import io.notifyhub.core.InMemoryNotificationTracker;
import io.notifyhub.core.NotificationListener;
import io.notifyhub.core.StatusWebhookListener;
import io.notifyhub.core.NotificationTracker;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.dedup.DeduplicationStore;
import io.notifyhub.core.dedup.InMemoryDeduplicationStore;
import io.notifyhub.core.ratelimit.RateLimitConfig;
import io.notifyhub.core.ratelimit.RateLimiter;
import io.notifyhub.core.ratelimit.TokenBucketRateLimiter;
import io.notifyhub.core.retry.RetryPolicy;
import io.notifyhub.core.template.MustacheTemplateEngine;
import io.notifyhub.core.template.TemplateEngine;
import io.notifyhub.spring.event.SpringEventNotificationListener;
import io.notifyhub.spring.metrics.MicrometerNotificationListener;
import io.notifyhub.spring.metrics.TracingNotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@AutoConfiguration
@EnableConfigurationProperties(NotifyProperties.class)
@Import({
    NotifySmsAutoConfiguration.class,
    NotifySlackAutoConfiguration.class,
    NotifyTelegramAutoConfiguration.class,
    NotifyDiscordAutoConfiguration.class,
    NotifyTeamsAutoConfiguration.class,
    NotifyFirebasePushAutoConfiguration.class,
    NotifyWebhookAutoConfiguration.class,
    NotifyWebSocketAutoConfiguration.class,
    NotifyGoogleChatAutoConfiguration.class,
    NotifyTwitterAutoConfiguration.class,
    NotifyLinkedInAutoConfiguration.class,
    NotifyNotionAutoConfiguration.class,
    NotifyTwitchAutoConfiguration.class,
    NotifyYouTubeAutoConfiguration.class,
    NotifyInstagramAutoConfiguration.class
})
public class NotifyAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NotifyAutoConfiguration.class);

    // ===================== TEMPLATE ENGINE =====================

    @Bean
    @ConditionalOnMissingBean(TemplateEngine.class)
    public TemplateEngine notifyTemplateEngine() {
        log.info("NotifyHub: Using Mustache template engine");
        return new MustacheTemplateEngine();
    }

    // ===================== SCHEDULING =====================

    @Bean
    @ConditionalOnMissingBean(ScheduledExecutorService.class)
    @ConditionalOnProperty(prefix = "notify.scheduling", name = "enabled", matchIfMissing = true)
    public ScheduledExecutorService notifyScheduler(NotifyProperties properties) {
        int poolSize = properties.getScheduling().getPoolSize();
        log.info("NotifyHub: Scheduler configured with pool size {}", poolSize);
        return Executors.newScheduledThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "notifyhub-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    // ===================== DELIVERY TRACKING =====================

    @Bean
    @ConditionalOnMissingBean(NotificationTracker.class)
    @ConditionalOnProperty(prefix = "notify.tracking", name = "enabled", havingValue = "true")
    public NotificationTracker notifyTracker() {
        log.info("NotifyHub: In-memory delivery tracking enabled");
        return new InMemoryNotificationTracker();
    }

    // ===================== DEDUPLICATION =====================

    @Bean
    @ConditionalOnMissingBean(DeduplicationStore.class)
    @ConditionalOnProperty(prefix = "notify.deduplication", name = "enabled", havingValue = "true")
    public DeduplicationStore deduplicationStore(NotifyProperties properties) {
        String ttlStr = properties.getDeduplication().getTtl();
        Duration ttl = parseDuration(ttlStr);
        log.info("NotifyHub: Deduplication enabled (TTL: {}, strategy: {})",
                ttl, properties.getDeduplication().getStrategy());
        return new InMemoryDeduplicationStore(ttl);
    }

    private static Duration parseDuration(String value) {
        if (value == null || value.isBlank()) return Duration.ofHours(24);
        value = value.trim().toLowerCase();
        if (value.endsWith("h")) return Duration.ofHours(Long.parseLong(value.replace("h", "")));
        if (value.endsWith("m")) return Duration.ofMinutes(Long.parseLong(value.replace("m", "")));
        if (value.endsWith("s")) return Duration.ofSeconds(Long.parseLong(value.replace("s", "")));
        if (value.endsWith("d")) return Duration.ofDays(Long.parseLong(value.replace("d", "")));
        return Duration.ofHours(Long.parseLong(value)); // default hours
    }

    // ===================== RATE LIMITING =====================

    @Bean
    @ConditionalOnProperty(prefix = "notify.rate-limit", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(RateLimiter.class)
    public RateLimiter notifyRateLimiter(NotifyProperties properties) {
        NotifyProperties.RateLimit rl = properties.getRateLimit();

        RateLimitConfig defaultConfig = new RateLimitConfig(
                rl.getMaxRequests(), parseDuration(rl.getWindow()));

        Map<String, RateLimitConfig> channelConfigs = new LinkedHashMap<>();

        // Apply API-aware defaults if enabled
        if (rl.isUseDefaults()) {
            channelConfigs.putAll(RateLimitConfig.allDefaults());
            log.info("NotifyHub: Rate limiting with API-aware defaults for {} channels",
                    channelConfigs.size());
        }

        // User overrides on top of defaults
        for (Map.Entry<String, NotifyProperties.ChannelRateLimit> entry : rl.getChannels().entrySet()) {
            NotifyProperties.ChannelRateLimit crl = entry.getValue();
            channelConfigs.put(entry.getKey(),
                    new RateLimitConfig(crl.getMaxRequests(), parseDuration(crl.getWindow())));
        }

        log.info("NotifyHub: Rate limiting enabled (default: {}/{}, channels: {})",
                rl.getMaxRequests(), rl.getWindow(), channelConfigs.keySet());

        return new TokenBucketRateLimiter(defaultConfig, channelConfigs);
    }

    // ===================== EMAIL CHANNEL =====================

    @Bean
    @ConditionalOnProperty(prefix = "notify.channels.email", name = "host")
    @ConditionalOnMissingBean(SmtpEmailChannel.class)
    public SmtpEmailChannel smtpEmailChannel(NotifyProperties properties) {
        NotifyProperties.Email email = properties.getChannels().getEmail();
        SmtpConfig config = SmtpConfig.builder()
                .host(email.getHost())
                .port(email.getPort())
                .username(email.getUsername())
                .password(email.getPassword())
                .from(email.getFrom())
                .fromName(email.getFromName())
                .tls(email.isTls())
                .ssl(email.isSsl())
                .build();
        log.info("NotifyHub: Email channel configured ({}:{})", email.getHost(), email.getPort());
        return new SmtpEmailChannel(config);
    }

    // ===================== MICROMETER METRICS (isolated to avoid ClassNotFound) =====================

    @AutoConfiguration
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    static class MicrometerConfiguration {
        @Bean
        @ConditionalOnBean(type = "io.micrometer.core.instrument.MeterRegistry")
        @ConditionalOnMissingBean(MicrometerNotificationListener.class)
        public MicrometerNotificationListener micrometerNotificationListener(
                io.micrometer.core.instrument.MeterRegistry meterRegistry) {
            LoggerFactory.getLogger(NotifyAutoConfiguration.class)
                    .info("NotifyHub: Micrometer metrics listener enabled");
            return new MicrometerNotificationListener(meterRegistry);
        }
    }

    // ===================== OPENTELEMETRY TRACING (isolated to avoid ClassNotFound) =====================

    @AutoConfiguration
    @ConditionalOnClass(name = "io.micrometer.observation.ObservationRegistry")
    static class ObservationConfiguration {
        @Bean
        @ConditionalOnBean(type = "io.micrometer.observation.ObservationRegistry")
        @ConditionalOnMissingBean(TracingNotificationListener.class)
        public TracingNotificationListener tracingNotificationListener(
                io.micrometer.observation.ObservationRegistry observationRegistry) {
            LoggerFactory.getLogger(NotifyAutoConfiguration.class)
                    .info("NotifyHub: OpenTelemetry tracing listener enabled");
            return new TracingNotificationListener(observationRegistry);
        }
    }

    // ===================== ACTUATOR (isolated to avoid ClassNotFound) =====================

    @AutoConfiguration
    @ConditionalOnClass(name = "org.springframework.boot.actuate.health.HealthIndicator")
    static class ActuatorConfiguration {
        @Bean
        @ConditionalOnProperty(prefix = "management.health.notifyhub", name = "enabled", matchIfMissing = true)
        @ConditionalOnMissingBean(name = "notifyHubHealthIndicator")
        public io.notifyhub.spring.actuator.NotifyHubHealthIndicator notifyHubHealthIndicator(
                ObjectProvider<List<NotificationChannel>> channelsProvider) {
            LoggerFactory.getLogger(NotifyAutoConfiguration.class)
                    .info("NotifyHub: Actuator health indicator enabled");
            return new io.notifyhub.spring.actuator.NotifyHubHealthIndicator(
                    channelsProvider.getIfAvailable(List::of));
        }

        @Bean
        @ConditionalOnProperty(prefix = "management.info.notifyhub", name = "enabled", matchIfMissing = true)
        @ConditionalOnMissingBean(name = "notifyHubInfoContributor")
        public io.notifyhub.spring.actuator.NotifyHubInfoContributor notifyHubInfoContributor(
                ObjectProvider<List<NotificationChannel>> channelsProvider,
                ObjectProvider<NotificationTracker> trackerProvider,
                NotifyProperties properties) {
            LoggerFactory.getLogger(NotifyAutoConfiguration.class)
                    .info("NotifyHub: Actuator info contributor enabled");
            return new io.notifyhub.spring.actuator.NotifyHubInfoContributor(
                    channelsProvider.getIfAvailable(List::of),
                    trackerProvider.getIfAvailable(),
                    properties.getTracking().isDlqEnabled());
        }
    }

    // ===================== AUDIT LOG =====================

    @Bean
    @ConditionalOnProperty(prefix = "notify.audit", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(AuditLog.class)
    public AuditLog inMemoryAuditLog() {
        log.info("NotifyHub: Audit log enabled (in-memory)");
        return new InMemoryAuditLog();
    }

    @Bean
    @ConditionalOnBean(AuditLog.class)
    @ConditionalOnMissingBean(AuditNotificationListener.class)
    public AuditNotificationListener auditNotificationListener(AuditLog auditLog) {
        log.info("NotifyHub: Audit notification listener enabled");
        return new AuditNotificationListener(auditLog);
    }

    // ===================== STATUS WEBHOOK =====================

    @Bean
    @ConditionalOnProperty(prefix = "notify.status-webhook", name = "url")
    @ConditionalOnMissingBean(StatusWebhookListener.class)
    public StatusWebhookListener statusWebhookListener(NotifyProperties properties) {
        NotifyProperties.StatusWebhook config = properties.getStatusWebhook();
        log.info("NotifyHub: Status webhook listener enabled (URL: {}, signing: {})",
                config.getUrl(), config.getSigningSecret() != null);
        return new StatusWebhookListener(
                config.getUrl(), config.getTimeoutMs(), config.getHeaders(), config.getSigningSecret());
    }

    // ===================== AUDIENCE / CONTACTS =====================

    @Bean
    @ConditionalOnProperty(prefix = "notify.audience", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(ContactRepository.class)
    public ContactRepository contactRepository() {
        log.info("NotifyHub: In-memory contact repository enabled");
        return new InMemoryContactRepository();
    }

    @Bean
    @ConditionalOnBean(ContactRepository.class)
    @ConditionalOnMissingBean(AudienceManager.class)
    public AudienceManager audienceManager(ContactRepository contactRepository) {
        log.info("NotifyHub: Audience manager enabled");
        return new AudienceManager(contactRepository);
    }

    // ===================== SPRING EVENTS =====================

    @Bean
    @ConditionalOnProperty(prefix = "notify.events", name = "enabled", matchIfMissing = true)
    @ConditionalOnMissingBean(SpringEventNotificationListener.class)
    public SpringEventNotificationListener springEventNotificationListener(ApplicationEventPublisher eventPublisher) {
        log.info("NotifyHub: Spring event notification listener enabled");
        return new SpringEventNotificationListener(eventPublisher);
    }

    // ===================== NOTIFY HUB =====================

    @Bean
    @ConditionalOnMissingBean(NotifyHub.class)
    public NotifyHub notifyHub(
            TemplateEngine templateEngine,
            ObjectProvider<List<NotificationChannel>> channelsProvider,
            ObjectProvider<List<NotificationListener>> listenersProvider,
            ObjectProvider<ExecutorService> executorProvider,
            ObjectProvider<ScheduledExecutorService> schedulerProvider,
            ObjectProvider<NotificationTracker> trackerProvider,
            ObjectProvider<DeduplicationStore> deduplicationStoreProvider,
            ObjectProvider<AuditLog> auditLogProvider,
            ObjectProvider<AudienceManager> audienceManagerProvider,
            ObjectProvider<RateLimiter> rateLimiterProvider,
            NotifyProperties properties) {

        NotifyHub.Builder builder = NotifyHub.builder()
                .templateEngine(templateEngine);

        // Register all discovered channels
        List<NotificationChannel> channels = channelsProvider.getIfAvailable(List::of);
        builder.channels(channels);
        log.info("NotifyHub: Registered {} channel(s): {}",
                channels.size(),
                channels.stream().map(NotificationChannel::getName).toList());

        // Register all discovered listeners
        List<NotificationListener> listeners = listenersProvider.getIfAvailable(List::of);
        listeners.forEach(builder::listener);

        // Configure retry policy
        NotifyProperties.Retry retry = properties.getRetry();
        if (retry.getMaxAttempts() > 1) {
            RetryPolicy policy = switch (retry.getStrategy().toLowerCase()) {
                case "exponential" -> RetryPolicy.exponential(retry.getMaxAttempts());
                case "fixed" -> RetryPolicy.fixed(retry.getMaxAttempts(), Duration.ofSeconds(1));
                default -> RetryPolicy.none();
            };
            builder.defaultRetryPolicy(policy);
            log.info("NotifyHub: Retry policy set to {} (max {} attempts)",
                    retry.getStrategy(), retry.getMaxAttempts());
        }

        // Configure async executor if available
        ExecutorService executor = executorProvider.getIfAvailable();
        if (executor != null) {
            builder.executor(executor);
            log.info("NotifyHub: Using provided ExecutorService for async operations");
        }

        // Configure scheduler for scheduled notifications
        ScheduledExecutorService scheduler = schedulerProvider.getIfAvailable();
        if (scheduler != null) {
            builder.scheduler(scheduler);
            log.info("NotifyHub: Scheduler configured for scheduled notifications");
        }

        // Configure delivery tracker
        NotificationTracker tracker = trackerProvider.getIfAvailable();
        if (tracker != null) {
            builder.tracker(tracker);
            log.info("NotifyHub: Delivery tracking enabled");
        }

        // Configure deduplication store
        DeduplicationStore dedupStore = deduplicationStoreProvider.getIfAvailable();
        if (dedupStore != null) {
            builder.deduplicationStore(dedupStore);
            log.info("NotifyHub: Deduplication store configured");
        }

        // Configure rate limiter
        RateLimiter rateLimiter = rateLimiterProvider.getIfAvailable();
        if (rateLimiter != null) {
            builder.rateLimiter(rateLimiter);
            log.info("NotifyHub: Rate limiter configured");
        }

        // Configure audit log
        AuditLog auditLog = auditLogProvider.getIfAvailable();
        if (auditLog != null) {
            builder.auditLog(auditLog);
            log.info("NotifyHub: Audit log configured");
        }

        // Configure audience manager
        AudienceManager audienceManager = audienceManagerProvider.getIfAvailable();
        if (audienceManager != null) {
            builder.audienceManager(audienceManager);
            log.info("NotifyHub: Audience manager configured");
        }

        return builder.build();
    }
}
