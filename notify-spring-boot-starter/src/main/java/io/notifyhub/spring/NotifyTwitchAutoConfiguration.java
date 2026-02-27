package io.notifyhub.spring;

import io.notifyhub.channel.twitch.TwitchChannel;
import io.notifyhub.channel.twitch.TwitchConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "io.notifyhub.channel.twitch.TwitchChannel")
public class NotifyTwitchAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NotifyTwitchAutoConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "notify.channels.twitch", name = "client-id")
    @ConditionalOnMissingBean(TwitchChannel.class)
    public TwitchChannel twitchChannel(NotifyProperties properties) {
        NotifyProperties.Twitch tw = properties.getChannels().getTwitch();
        TwitchConfig config = TwitchConfig.builder()
                .clientId(tw.getClientId())
                .accessToken(tw.getAccessToken())
                .broadcasterId(tw.getBroadcasterId())
                .senderId(tw.getSenderId())
                .recipients(tw.getRecipients())
                .build();
        log.info("NotifyHub: Twitch channel configured");
        return new TwitchChannel(config);
    }
}
