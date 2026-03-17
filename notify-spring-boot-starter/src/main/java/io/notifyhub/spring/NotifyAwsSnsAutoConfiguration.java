package io.notifyhub.spring;

import io.notifyhub.channel.sns.AwsSnsChannel;
import io.notifyhub.channel.sns.AwsSnsConfig;
import io.notifyhub.spring.properties.AwsSnsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AWS SNS auto-configuration — isolated so classes are only loaded
 * when notify-aws-sns module is on the classpath.
 */
@Configuration
@ConditionalOnClass(name = "io.notifyhub.channel.sns.AwsSnsChannel")
public class NotifyAwsSnsAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NotifyAwsSnsAutoConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "notify.channels.aws-sns", name = "region")
    @ConditionalOnMissingBean(AwsSnsChannel.class)
    public AwsSnsChannel awsSnsChannel(NotifyProperties properties) {
        AwsSnsProperties sns = properties.getChannels().getAwsSns();
        AwsSnsConfig.Builder builder = AwsSnsConfig.builder()
                .region(sns.getRegion())
                .accessKeyId(sns.getAccessKeyId())
                .secretAccessKey(sns.getSecretAccessKey());
        if (sns.getTopicArn() != null) {
            builder.topicArn(sns.getTopicArn());
        }
        log.info("NotifyHub: AWS SNS channel configured (region: {})", sns.getRegion());
        return new AwsSnsChannel(builder.build());
    }
}
