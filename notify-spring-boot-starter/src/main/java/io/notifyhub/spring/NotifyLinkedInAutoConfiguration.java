package io.notifyhub.spring;

import io.notifyhub.channel.linkedin.LinkedInChannel;
import io.notifyhub.channel.linkedin.LinkedInConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "io.notifyhub.channel.linkedin.LinkedInChannel")
public class NotifyLinkedInAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NotifyLinkedInAutoConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "notify.channels.linkedin", name = "access-token")
    @ConditionalOnMissingBean(LinkedInChannel.class)
    public LinkedInChannel linkedInChannel(NotifyProperties properties) {
        NotifyProperties.LinkedIn li = properties.getChannels().getLinkedin();
        LinkedInConfig config = LinkedInConfig.builder()
                .accessToken(li.getAccessToken())
                .authorId(li.getAuthorId())
                .recipients(li.getRecipients())
                .build();
        log.info("NotifyHub: LinkedIn channel configured");
        return new LinkedInChannel(config);
    }
}
