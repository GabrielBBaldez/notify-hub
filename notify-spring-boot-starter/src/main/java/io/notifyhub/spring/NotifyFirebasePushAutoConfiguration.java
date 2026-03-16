package io.notifyhub.spring;

import io.notifyhub.channel.push.firebase.FirebasePushChannel;
import io.notifyhub.channel.push.firebase.FirebasePushConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Firebase Push auto-configuration — isolated so classes are only loaded
 * when notify-push-firebase module is on the classpath.
 */
@Configuration
@ConditionalOnClass(name = "io.notifyhub.channel.push.firebase.FirebasePushChannel")
public class NotifyFirebasePushAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NotifyFirebasePushAutoConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "notify.channels.push", name = "project-id")
    @ConditionalOnMissingBean(FirebasePushChannel.class)
    public FirebasePushChannel firebasePushChannel(NotifyProperties properties) {
        NotifyProperties.Push push = properties.getChannels().getPush();
        FirebasePushConfig config = FirebasePushConfig.builder()
                .projectId(push.getProjectId())
                .serviceAccountJson(push.getServiceAccountJson())
                .build();
        log.info("NotifyHub: Firebase Push channel configured");
        return new FirebasePushChannel(config);
    }
}
