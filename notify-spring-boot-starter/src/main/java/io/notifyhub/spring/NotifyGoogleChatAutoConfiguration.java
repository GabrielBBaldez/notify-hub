package io.notifyhub.spring;

import io.notifyhub.channel.googlechat.GoogleChatChannel;
import io.notifyhub.channel.googlechat.GoogleChatConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Google Chat auto-configuration — isolated so classes are only loaded
 * when notify-google-chat module is on the classpath.
 */
@Configuration
@ConditionalOnClass(name = "io.notifyhub.channel.googlechat.GoogleChatChannel")
public class NotifyGoogleChatAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NotifyGoogleChatAutoConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "notify.channels.google-chat", name = "webhook-url")
    @ConditionalOnMissingBean(GoogleChatChannel.class)
    public GoogleChatChannel googleChatChannel(NotifyProperties properties) {
        NotifyProperties.GoogleChat gc = properties.getChannels().getGoogleChat();
        GoogleChatConfig config = GoogleChatConfig.builder()
                .webhookUrl(gc.getWebhookUrl())
                .timeoutMs(gc.getTimeoutMs())
                .build();
        log.info("NotifyHub: Google Chat channel configured (webhook)");
        return new GoogleChatChannel(config);
    }
}
