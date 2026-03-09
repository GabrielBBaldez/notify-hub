package io.notifyhub.spring;

import io.notifyhub.channel.slack.SlackChannel;
import io.notifyhub.channel.slack.SlackConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Slack auto-configuration — isolated so classes are only loaded
 * when notify-slack module is on the classpath.
 */
@Configuration
@ConditionalOnClass(name = "io.notifyhub.channel.slack.SlackChannel")
public class NotifySlackAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NotifySlackAutoConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "notify.channels.slack", name = "webhook-url")
    @ConditionalOnMissingBean(SlackChannel.class)
    public SlackChannel slackChannel(NotifyProperties properties) {
        NotifyProperties.Slack slack = properties.getChannels().getSlack();
        SlackConfig.Builder builder = SlackConfig.builder()
                .webhookUrl(slack.getWebhookUrl())
                .recipients(slack.getRecipients());
        if (slack.getUsername() != null) builder.username(slack.getUsername());
        if (slack.getIconUrl() != null) builder.iconUrl(slack.getIconUrl());
        SlackConfig config = builder.build();
        log.info("NotifyHub: Slack channel configured (webhook)");
        return new SlackChannel(config);
    }
}
