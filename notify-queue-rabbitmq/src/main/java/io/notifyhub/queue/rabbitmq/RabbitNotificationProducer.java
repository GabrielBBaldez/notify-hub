package io.notifyhub.queue.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Publishes notifications to a RabbitMQ exchange for async processing.
 *
 * <pre>{@code
 * producer.enqueue(QueuedNotification.builder()
 *     .recipient("user@example.com")
 *     .channelName("email")
 *     .subject("Welcome!")
 *     .rawContent("Hello from NotifyHub")
 *     .build());
 * }</pre>
 */
public class RabbitNotificationProducer {

    private static final Logger log = LoggerFactory.getLogger(RabbitNotificationProducer.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeName;
    private final String routingKey;

    public RabbitNotificationProducer(RabbitTemplate rabbitTemplate, String exchangeName, String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
    }

    /**
     * Enqueue a notification for async delivery.
     */
    public void enqueue(QueuedNotification notification) {
        log.info("Enqueuing notification: channel={}, recipient={}", notification.getChannelName(), notification.getRecipient());
        rabbitTemplate.convertAndSend(exchangeName, routingKey, notification);
        log.debug("Notification enqueued successfully: {}", notification);
    }
}
