package io.notifyhub.spring;

import io.notifyhub.channel.facebook.FacebookChannel;
import io.notifyhub.channel.facebook.FacebookConfig;
import io.notifyhub.spring.properties.FacebookProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "io.notifyhub.channel.facebook.FacebookChannel")
public class NotifyFacebookAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NotifyFacebookAutoConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "notify.channels.facebook", name = "page-access-token")
    @ConditionalOnMissingBean(FacebookChannel.class)
    public FacebookChannel facebookChannel(NotifyProperties properties) {
        FacebookProperties fb = properties.getChannels().getFacebook();
        FacebookConfig config = FacebookConfig.builder()
                .pageAccessToken(fb.getPageAccessToken())
                .pageId(fb.getPageId())
                .recipients(fb.getRecipients())
                .build();
        log.info("NotifyHub: Facebook channel configured");
        return new FacebookChannel(config);
    }
}
