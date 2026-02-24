package io.notifyhub.spring;

import io.notifyhub.channel.discord.DiscordChannel;
import io.notifyhub.channel.discord.DiscordConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Discord auto-configuration — isolated so classes are only loaded
 * when notify-discord module is on the classpath.
 */
@Configuration
@ConditionalOnClass(name = "io.notifyhub.channel.discord.DiscordChannel")
public class NotifyDiscordAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NotifyDiscordAutoConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "notify.channels.discord", name = "webhook-url")
    @ConditionalOnMissingBean(DiscordChannel.class)
    public DiscordChannel discordChannel(NotifyProperties properties) {
        NotifyProperties.Discord dc = properties.getChannels().getDiscord();
        DiscordConfig.Builder builder = DiscordConfig.builder()
                .webhookUrl(dc.getWebhookUrl());
        if (dc.getUsername() != null) {
            builder.username(dc.getUsername());
        }
        if (dc.getAvatarUrl() != null) {
            builder.avatarUrl(dc.getAvatarUrl());
        }
        log.info("NotifyHub: Discord channel configured (webhook)");
        return new DiscordChannel(builder.build());
    }
}
