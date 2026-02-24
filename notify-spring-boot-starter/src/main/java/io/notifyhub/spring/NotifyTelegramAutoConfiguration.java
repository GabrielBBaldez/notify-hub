package io.notifyhub.spring;

import io.notifyhub.channel.telegram.TelegramChannel;
import io.notifyhub.channel.telegram.TelegramConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Telegram auto-configuration — isolated so classes are only loaded
 * when notify-telegram module is on the classpath.
 */
@Configuration
@ConditionalOnClass(name = "io.notifyhub.channel.telegram.TelegramChannel")
public class NotifyTelegramAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NotifyTelegramAutoConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "notify.channels.telegram", name = "bot-token")
    @ConditionalOnMissingBean(TelegramChannel.class)
    public TelegramChannel telegramChannel(NotifyProperties properties) {
        NotifyProperties.Telegram tg = properties.getChannels().getTelegram();
        TelegramConfig.Builder builder = TelegramConfig.builder()
                .botToken(tg.getBotToken());
        if (tg.getChatId() != null) {
            builder.defaultChatId(tg.getChatId());
        }
        log.info("NotifyHub: Telegram channel configured (Bot API)");
        return new TelegramChannel(builder.build());
    }
}
