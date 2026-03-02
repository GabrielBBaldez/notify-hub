package io.notifyhub.spring;

import io.notifyhub.channel.instagram.InstagramChannel;
import io.notifyhub.channel.instagram.InstagramConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Instagram auto-configuration — isolated so classes are only loaded
 * when notify-instagram module is on the classpath.
 */
@Configuration
@ConditionalOnClass(name = "io.notifyhub.channel.instagram.InstagramChannel")
public class NotifyInstagramAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NotifyInstagramAutoConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "notify.channels.instagram", name = "access-token")
    @ConditionalOnMissingBean(InstagramChannel.class)
    public InstagramChannel instagramChannel(NotifyProperties properties) {
        NotifyProperties.Instagram ig = properties.getChannels().getInstagram();
        InstagramConfig.Builder builder = InstagramConfig.builder()
                .accessToken(ig.getAccessToken())
                .igUserId(ig.getIgUserId())
                .recipients(ig.getRecipients());
        log.info("NotifyHub: Instagram channel configured (Graph API)");
        return new InstagramChannel(builder.build());
    }
}
