package io.notifyhub.spring;

import io.notifyhub.channel.email.SmtpConfig;
import io.notifyhub.channel.email.SmtpEmailChannel;
import io.notifyhub.core.NotifyHub;
import io.notifyhub.core.NotificationListener;
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

/**
 * Spring Boot auto-configuration for NotifyHub.
 *
 * <p>Automatically configures:</p>
 * <ul>
 *   <li>Email channel (when {@code notify.channels.email.host} is set)</li>
 *   <li>SMS channel (when Twilio is on classpath and {@code notify.channels.sms.account-sid} is set)</li>
 *   <li>Mustache template engine (default)</li>
 *   <li>Retry policy (when {@code notify.retry} is configured)</li>
 * </ul>
 *
 * <p>Custom channels implementing {@link NotificationChannel} are automatically
 * discovered and registered as Spring beans.</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(NotifyProperties.class)
@Import(NotifySmsAutoConfiguration.class)
public class NotifyAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NotifyAutoConfiguration.class);

    // ===================== TEMPLATE ENGINE =====================

    @Bean
    @ConditionalOnMissingBean(TemplateEngine.class)
    public TemplateEngine notifyTemplateEngine() {
        log.info("NotifyHub: Using Mustache template engine");
        return new MustacheTemplateEngine();
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

        return builder.build();
    }
}
