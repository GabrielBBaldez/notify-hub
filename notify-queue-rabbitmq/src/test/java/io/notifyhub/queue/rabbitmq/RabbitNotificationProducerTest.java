package io.notifyhub.queue.rabbitmq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitNotificationProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("enqueue sends notification to correct exchange and routing key")
    void enqueueSendsToExchange() {
        RabbitNotificationProducer producer = new RabbitNotificationProducer(
                rabbitTemplate, "notifyhub-exchange", "notification");

        QueuedNotification notification = QueuedNotification.builder()
                .recipient("user@test.com")
                .channelName("email")
                .subject("Test")
                .rawContent("Hello from queue")
                .build();

        producer.enqueue(notification);

        verify(rabbitTemplate).convertAndSend("notifyhub-exchange", "notification", notification);
    }
}
