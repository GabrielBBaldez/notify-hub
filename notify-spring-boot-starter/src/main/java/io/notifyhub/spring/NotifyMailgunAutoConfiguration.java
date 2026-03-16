package io.notifyhub.spring;

import io.notifyhub.channel.mailgun.MailgunChannel;
import io.notifyhub.channel.mailgun.MailgunConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Mailgun auto-configuration — isolated so classes are only loaded
 * when notify-mailgun module is on the classpath.
 */
@Configuration
@ConditionalOnClass(name = "io.notifyhub.channel.mailgun.MailgunChannel")
public class NotifyMailgunAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NotifyMailgunAutoConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "notify.channels.mailgun", name = "api-key")
    @ConditionalOnMissingBean(MailgunChannel.class)
    public MailgunChannel mailgunChannel(NotifyProperties properties) {
        NotifyProperties.Mailgun mg = properties.getChannels().getMailgun();
        MailgunConfig.Builder builder = MailgunConfig.builder()
                .apiKey(mg.getApiKey())
                .domain(mg.getDomain())
                .from(mg.getFrom());
        if (mg.getRegion() != null && !mg.getRegion().isBlank()) {
            builder.region(mg.getRegion());
        }
        log.info("NotifyHub: Mailgun channel configured (domain: {}, region: {})",
                mg.getDomain(), mg.getRegion() != null ? mg.getRegion() : "US");
        return new MailgunChannel(builder.build());
    }
}
