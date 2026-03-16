package io.notifyhub.spring;

import io.notifyhub.channel.pagerduty.PagerDutyChannel;
import io.notifyhub.channel.pagerduty.PagerDutyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * PagerDuty auto-configuration — isolated so classes are only loaded
 * when notify-pagerduty module is on the classpath.
 */
@Configuration
@ConditionalOnClass(name = "io.notifyhub.channel.pagerduty.PagerDutyChannel")
public class NotifyPagerDutyAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NotifyPagerDutyAutoConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "notify.channels.pagerduty", name = "routing-key")
    @ConditionalOnMissingBean(PagerDutyChannel.class)
    public PagerDutyChannel pagerDutyChannel(NotifyProperties properties) {
        NotifyProperties.PagerDuty pd = properties.getChannels().getPagerduty();
        PagerDutyConfig.Builder builder = PagerDutyConfig.builder()
                .routingKey(pd.getRoutingKey());
        if (pd.getSeverity() != null) {
            builder.severity(pd.getSeverity());
        }
        log.info("NotifyHub: PagerDuty channel configured");
        return new PagerDutyChannel(builder.build());
    }
}
