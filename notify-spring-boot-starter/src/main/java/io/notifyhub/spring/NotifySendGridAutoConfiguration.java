package io.notifyhub.spring;

import io.notifyhub.channel.sendgrid.SendGridConfig;
import io.notifyhub.channel.sendgrid.SendGridEmailChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the SendGrid email channel.
 *
 * <p>Activated when {@code notify.channels.sendgrid.api-key} is set.
 * When active, this replaces the SMTP email channel — both register
 * with the name "email", so only one can be active.</p>
 *
 * <pre>
 * notify:
 *   channels:
 *     sendgrid:
 *       api-key: ${SENDGRID_API_KEY}
 *       from: noreply@myapp.com
 *       from-name: MyApp
 *       track-opens: true
 *       track-clicks: true
 * </pre>
 */
@AutoConfiguration
@ConditionalOnClass(name = "io.notifyhub.channel.sendgrid.SendGridEmailChannel")
public class NotifySendGridAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NotifySendGridAutoConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "notify.channels.sendgrid", name = "api-key")
    @ConditionalOnMissingBean(SendGridEmailChannel.class)
    public SendGridEmailChannel sendGridEmailChannel(NotifyProperties properties) {
        NotifyProperties.SendGrid sg = properties.getChannels().getSendgrid();
        SendGridConfig config = SendGridConfig.builder()
                .apiKey(sg.getApiKey())
                .from(sg.getFrom())
                .fromName(sg.getFromName())
                .trackOpens(sg.isTrackOpens())
                .trackClicks(sg.isTrackClicks())
                .build();
        log.info("NotifyHub: SendGrid email channel configured (from: {}, tracking: opens={}, clicks={})",
                sg.getFrom(), sg.isTrackOpens(), sg.isTrackClicks());
        return new SendGridEmailChannel(config);
    }
}
