package io.notifyhub.queue.kafka;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaNotificationProducerTest {

    @Mock
    private KafkaTemplate<String, QueuedNotification> kafkaTemplate;

    @Test
    @DisplayName("enqueue sends notification to correct topic with channel:recipient key")
    void enqueueSendsToTopic() {
        KafkaNotificationProducer producer = new KafkaNotificationProducer(kafkaTemplate, "notifyhub-notifications");

        QueuedNotification notification = QueuedNotification.builder()
                .recipient("user@test.com")
                .channelName("email")
                .subject("Test")
                .rawContent("Hello from Kafka")
                .build();

        producer.enqueue(notification);

        verify(kafkaTemplate).send("notifyhub-notifications", "email:user@test.com", notification);
    }
}
