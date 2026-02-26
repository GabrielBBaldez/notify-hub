package io.notifyhub.queue.kafka;

import io.notifyhub.queue.kafka.QueuedNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Publishes notifications to a Kafka topic for async processing.
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
public class KafkaNotificationProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaNotificationProducer.class);

    private final KafkaTemplate<String, QueuedNotification> kafkaTemplate;
    private final String topic;

    public KafkaNotificationProducer(KafkaTemplate<String, QueuedNotification> kafkaTemplate, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    /**
     * Enqueue a notification for async delivery via Kafka.
     */
    public void enqueue(QueuedNotification notification) {
        log.info("Enqueuing notification to Kafka: channel={}, recipient={}", notification.getChannelName(), notification.getRecipient());
        String key = notification.getChannelName() + ":" + notification.getRecipient();
        kafkaTemplate.send(topic, key, notification);
        log.debug("Notification enqueued successfully: {}", notification);
    }
}
