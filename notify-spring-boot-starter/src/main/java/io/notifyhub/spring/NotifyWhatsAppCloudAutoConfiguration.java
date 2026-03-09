package io.notifyhub.spring;

import io.notifyhub.channel.whatsapp.WhatsAppCloudChannel;
import io.notifyhub.channel.whatsapp.WhatsAppCloudConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "io.notifyhub.channel.whatsapp.WhatsAppCloudChannel")
public class NotifyWhatsAppCloudAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NotifyWhatsAppCloudAutoConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "notify.channels.whatsapp", name = "access-token")
    @ConditionalOnMissingBean(WhatsAppCloudChannel.class)
    public WhatsAppCloudChannel whatsAppCloudChannel(NotifyProperties properties) {
        NotifyProperties.WhatsApp wa = properties.getChannels().getWhatsapp();
        WhatsAppCloudConfig.Builder builder = WhatsAppCloudConfig.builder()
                .accessToken(wa.getAccessToken())
                .phoneNumberId(wa.getPhoneNumberId())
                .recipients(wa.getRecipients());
        if (wa.getApiVersion() != null) {
            builder.apiVersion(wa.getApiVersion());
        }
        log.info("NotifyHub: WhatsApp channel configured (Cloud API)");
        return new WhatsAppCloudChannel(builder.build());
    }
}
