package io.notifyhub.spring;

import io.notifyhub.channel.sms.TwilioConfig;
import io.notifyhub.channel.sms.TwilioSmsChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SMS auto-configuration — isolated in its own class so that
 * the Twilio classes are only loaded when Twilio SDK is on the classpath.
 */
@Configuration
@ConditionalOnClass(name = "com.twilio.Twilio")
public class NotifySmsAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NotifySmsAutoConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "notify.channels.sms", name = "account-sid")
    @ConditionalOnMissingBean(TwilioSmsChannel.class)
    public TwilioSmsChannel twilioSmsChannel(NotifyProperties properties) {
        NotifyProperties.Sms sms = properties.getChannels().getSms();
        TwilioConfig config = TwilioConfig.builder()
                .accountSid(sms.getAccountSid())
                .authToken(sms.getAuthToken())
                .fromNumber(sms.getFromNumber())
                .build();
        log.info("NotifyHub: SMS channel configured (Twilio)");
        return new TwilioSmsChannel(config);
    }
}
