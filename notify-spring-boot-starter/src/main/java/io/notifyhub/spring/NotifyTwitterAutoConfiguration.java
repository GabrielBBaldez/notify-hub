package io.notifyhub.spring;

import io.notifyhub.channel.twitter.TwitterChannel;
import io.notifyhub.channel.twitter.TwitterConfig;
import io.notifyhub.spring.properties.TwitterProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "io.notifyhub.channel.twitter.TwitterChannel")
public class NotifyTwitterAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NotifyTwitterAutoConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "notify.channels.twitter", name = "api-key")
    @ConditionalOnMissingBean(TwitterChannel.class)
    public TwitterChannel twitterChannel(NotifyProperties properties) {
        TwitterProperties tw = properties.getChannels().getTwitter();
        TwitterConfig config = TwitterConfig.builder()
                .apiKey(tw.getApiKey())
                .apiSecret(tw.getApiSecret())
                .accessToken(tw.getAccessToken())
                .accessTokenSecret(tw.getAccessTokenSecret())
                .recipients(tw.getRecipients())
                .build();
        log.info("NotifyHub: Twitter/X channel configured");
        return new TwitterChannel(config);
    }
}
