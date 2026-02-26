package io.notifyhub.queue.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.notifyhub.core.NotifyHub;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for NotifyHub RabbitMQ queue integration.
 * Activated when {@code notify.queue.rabbitmq.enabled=true} and RabbitMQ is on the classpath.
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
@ConditionalOnProperty(prefix = "notify.queue.rabbitmq", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(RabbitQueueProperties.class)
public class RabbitQueueAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "notifyHubQueue")
    public Queue notifyHubQueue(RabbitQueueProperties props) {
        return QueueBuilder.durable(props.getQueueName()).build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "notifyHubExchange")
    public DirectExchange notifyHubExchange(RabbitQueueProperties props) {
        return new DirectExchange(props.getExchangeName());
    }

    @Bean
    @ConditionalOnMissingBean(name = "notifyHubBinding")
    public Binding notifyHubBinding(Queue notifyHubQueue, DirectExchange notifyHubExchange, RabbitQueueProperties props) {
        return BindingBuilder.bind(notifyHubQueue).to(notifyHubExchange).with(props.getRoutingKey());
    }

    @Bean
    @ConditionalOnMissingBean(name = "notifyHubMessageConverter")
    public MessageConverter notifyHubMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    @ConditionalOnMissingBean(RabbitNotificationProducer.class)
    public RabbitNotificationProducer notifyProducer(RabbitTemplate rabbitTemplate, RabbitQueueProperties props) {
        return new RabbitNotificationProducer(rabbitTemplate, props.getExchangeName(), props.getRoutingKey());
    }

    @Bean
    @ConditionalOnMissingBean(RabbitNotificationConsumer.class)
    @ConditionalOnProperty(prefix = "notify.queue.rabbitmq.consumer", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RabbitNotificationConsumer notifyConsumer(NotifyHub hub) {
        return new RabbitNotificationConsumer(hub);
    }
}
