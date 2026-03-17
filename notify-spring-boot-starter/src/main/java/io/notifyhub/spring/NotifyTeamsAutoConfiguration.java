package io.notifyhub.spring;

import io.notifyhub.channel.teams.TeamsChannel;
import io.notifyhub.channel.teams.TeamsConfig;
import io.notifyhub.spring.properties.TeamsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Microsoft Teams auto-configuration — isolated so classes are only loaded
 * when notify-teams module is on the classpath.
 */
@Configuration
@ConditionalOnClass(name = "io.notifyhub.channel.teams.TeamsChannel")
public class NotifyTeamsAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NotifyTeamsAutoConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "notify.channels.teams", name = "webhook-url")
    @ConditionalOnMissingBean(TeamsChannel.class)
    public TeamsChannel teamsChannel(NotifyProperties properties) {
        TeamsProperties teams = properties.getChannels().getTeams();
        TeamsConfig config = TeamsConfig.builder()
                .webhookUrl(teams.getWebhookUrl())
                .recipients(teams.getRecipients())
                .build();
        log.info("NotifyHub: Teams channel configured (webhook)");
        return new TeamsChannel(config);
    }
}
