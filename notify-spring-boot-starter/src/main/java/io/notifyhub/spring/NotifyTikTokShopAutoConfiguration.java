package io.notifyhub.spring;

import io.notifyhub.channel.tiktokshop.TikTokShopChannel;
import io.notifyhub.channel.tiktokshop.TikTokShopConfig;
import io.notifyhub.spring.properties.TikTokShopProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "io.notifyhub.channel.tiktokshop.TikTokShopChannel")
public class NotifyTikTokShopAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NotifyTikTokShopAutoConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "notify.channels.tiktok-shop", name = "app-key")
    @ConditionalOnMissingBean(TikTokShopChannel.class)
    public TikTokShopChannel tikTokShopChannel(NotifyProperties properties) {
        TikTokShopProperties tt = properties.getChannels().getTiktokShop();
        TikTokShopConfig config = TikTokShopConfig.builder()
                .appKey(tt.getAppKey())
                .appSecret(tt.getAppSecret())
                .accessToken(tt.getAccessToken())
                .shopId(tt.getShopId())
                .recipients(tt.getRecipients())
                .build();
        log.info("NotifyHub: TikTok Shop channel configured");
        return new TikTokShopChannel(config);
    }
}
