package io.notifyhub.spring;

import io.notifyhub.channel.kick.KickChannel;
import io.notifyhub.channel.kick.KickConfig;
import io.notifyhub.spring.properties.KickProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "io.notifyhub.channel.kick.KickChannel")
public class NotifyKickAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NotifyKickAutoConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "notify.channels.kick", name = "client-id")
    @ConditionalOnMissingBean(KickChannel.class)
    public KickChannel kickChannel(NotifyProperties properties) {
        KickProperties k = properties.getChannels().getKick();
        KickConfig.Builder builder = KickConfig.builder()
                .clientId(k.getClientId())
                .accessToken(k.getAccessToken())
                .broadcasterId(k.getBroadcasterId());
        if (k.getMessageType() != null) builder.messageType(k.getMessageType());
        if (k.getRecipients() != null) builder.recipients(k.getRecipients());
        if (k.getTimeoutMs() > 0) builder.timeoutMs(k.getTimeoutMs());
        if (k.getRefreshToken() != null) builder.refreshToken(k.getRefreshToken());
        if (k.getClientSecret() != null) builder.clientSecret(k.getClientSecret());
        log.info("NotifyHub: Kick channel configured (token-refresh: {})",
                k.getRefreshToken() != null);
        return new KickChannel(builder.build());
    }
}
