package io.notifyhub.queue.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.notifyhub.core.NotifyHub;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 * Auto-configuration for NotifyHub Kafka queue integration.
 * Activated when {@code notify.queue.kafka.enabled=true} and Kafka is on the classpath.
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
@ConditionalOnProperty(prefix = "notify.queue.kafka", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(KafkaQueueProperties.class)
public class KafkaQueueAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(KafkaNotificationProducer.class)
    public KafkaNotificationProducer notifyKafkaProducer(KafkaTemplate<String, QueuedNotification> kafkaTemplate, KafkaQueueProperties props) {
        return new KafkaNotificationProducer(kafkaTemplate, props.getTopic());
    }

    @Bean
    @ConditionalOnMissingBean(KafkaNotificationConsumer.class)
    @ConditionalOnProperty(prefix = "notify.queue.kafka.consumer", name = "enabled", havingValue = "true", matchIfMissing = true)
    public KafkaNotificationConsumer notifyKafkaConsumer(NotifyHub hub) {
        return new KafkaNotificationConsumer(hub);
    }
}
