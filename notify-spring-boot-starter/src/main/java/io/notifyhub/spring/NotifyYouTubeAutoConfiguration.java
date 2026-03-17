package io.notifyhub.spring;

import io.notifyhub.channel.youtube.YouTubeChannel;
import io.notifyhub.channel.youtube.YouTubeConfig;
import io.notifyhub.spring.properties.YouTubeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "io.notifyhub.channel.youtube.YouTubeChannel")
public class NotifyYouTubeAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NotifyYouTubeAutoConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "notify.channels.youtube", name = "access-token")
    @ConditionalOnMissingBean(YouTubeChannel.class)
    public YouTubeChannel youTubeChannel(NotifyProperties properties) {
        YouTubeProperties yt = properties.getChannels().getYoutube();
        YouTubeConfig.Builder builder = YouTubeConfig.builder()
                .accessToken(yt.getAccessToken());
        if (yt.getChannelId() != null) builder.channelId(yt.getChannelId());
        if (yt.getLiveChatId() != null) builder.liveChatId(yt.getLiveChatId());
        if (yt.getRecipients() != null) builder.recipients(yt.getRecipients());
        // OAuth token refresh
        if (yt.getRefreshToken() != null) builder.refreshToken(yt.getRefreshToken());
        if (yt.getClientId() != null) builder.clientId(yt.getClientId());
        if (yt.getClientSecret() != null) builder.clientSecret(yt.getClientSecret());
        log.info("NotifyHub: YouTube channel configured (token-refresh: {})",
                yt.getRefreshToken() != null);
        return new YouTubeChannel(builder.build());
    }
}
