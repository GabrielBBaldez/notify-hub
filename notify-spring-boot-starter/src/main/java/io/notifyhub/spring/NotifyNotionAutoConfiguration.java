package io.notifyhub.spring;

import io.notifyhub.channel.notion.NotionChannel;
import io.notifyhub.channel.notion.NotionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "io.notifyhub.channel.notion.NotionChannel")
public class NotifyNotionAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NotifyNotionAutoConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "notify.channels.notion", name = "api-key")
    @ConditionalOnMissingBean(NotionChannel.class)
    public NotionChannel notionChannel(NotifyProperties properties) {
        NotifyProperties.Notion nt = properties.getChannels().getNotion();
        NotionConfig config = NotionConfig.builder()
                .apiKey(nt.getApiKey())
                .databaseId(nt.getDatabaseId())
                .recipients(nt.getRecipients())
                .build();
        log.info("NotifyHub: Notion channel configured");
        return new NotionChannel(config);
    }
}
