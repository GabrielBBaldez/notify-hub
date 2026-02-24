package io.notifyhub.spring;

import io.notifyhub.channel.email.SmtpConfig;
import io.notifyhub.channel.email.SmtpEmailChannel;
import io.notifyhub.core.NotifyHub;
import io.notifyhub.core.InMemoryNotificationTracker;
import io.notifyhub.core.NotificationListener;
import io.notifyhub.core.NotificationTracker;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.retry.RetryPolicy;
import io.notifyhub.core.template.MustacheTemplateEngine;
import io.notifyhub.core.template.TemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Spring Boot auto-configuration for NotifyHub.
 *
 * <p>Automatically configures:</p>
 * <ul>
 *   <li>Email channel (when {@code notify.channels.email.host} is set)</li>
 *   <li>SMS channel (when Twilio is on classpath and {@code notify.channels.sms.account-sid} is set)</li>
 *   <li>Slack channel (when notify-slack is on classpath and {@code notify.channels.slack.webhook-url} is set)</li>
 *   <li>Telegram channel (when notify-telegram is on classpath and {@code notify.channels.telegram.bot-token} is set)</li>
 *   <li>Discord channel (when notify-discord is on classpath and {@code notify.channels.discord.webhook-url} is set)</li>
 *   <li>Mustache template engine (default)</li>
 *   <li>Retry policy (when {@code notify.retry} is configured)</li>
 *   <li>Scheduled notifications (when {@code notify.scheduling.enabled=true}, default)</li>
 *   <li>Delivery tracking (when {@code notify.tracking.enabled=true})</li>
 * </ul>
 *
 * <p>Custom channels implementing {@link NotificationChannel} are automatically
 * discovered and registered as Spring beans.</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(NotifyProperties.class)
@Import({
    NotifySmsAutoConfiguration.class,
    NotifySlackAutoConfiguration.class,
    NotifyTelegramAutoConfiguration.class,
    NotifyDiscordAutoConfiguration.class
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

        return builder.build();
    }
}
