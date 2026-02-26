package io.notifyhub.queue.rabbitmq;

import io.notifyhub.core.ChannelRef;
import io.notifyhub.core.NotificationBuilder;
import io.notifyhub.core.NotifyHub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RabbitNotificationConsumerTest {

    @Mock private NotifyHub hub;
    @Mock private NotificationBuilder builder;

    private RabbitNotificationConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new RabbitNotificationConsumer(hub);
        when(hub.to(anyString())).thenReturn(builder);
        when(hub.toPhone(anyString())).thenReturn(builder);
        when(builder.via(any(ChannelRef.class))).thenReturn(builder);
        when(builder.subject(anyString())).thenReturn(builder);
        when(builder.content(anyString())).thenReturn(builder);
        when(builder.template(anyString())).thenReturn(builder);
        when(builder.params(any())).thenReturn(builder);
        when(builder.priority(any())).thenReturn(builder);
        when(builder.deduplicationKey(anyString())).thenReturn(builder);
    }

    @Test
    @DisplayName("Consumes email notification with raw content")
    void consumeEmailWithContent() {
        QueuedNotification n = QueuedNotification.builder()
                .recipient("user@test.com")
                .channelName("email")
                .subject("Hello")
                .rawContent("Welcome!")
                .build();

        consumer.onMessage(n);

        verify(hub).to("user@test.com");
        verify(builder).subject("Hello");
        verify(builder).content("Welcome!");
        verify(builder).sendTracked();
    }

    @Test
    @DisplayName("Consumes notification with template")
    void consumeWithTemplate() {
        QueuedNotification n = QueuedNotification.builder()
                .recipient("user@test.com")
                .channelName("email")
                .templateName("welcome")
                .params(Map.of("name", "Gabriel"))
                .build();

        consumer.onMessage(n);

        verify(builder).template("welcome");
        verify(builder).params(Map.of("name", "Gabriel"));
        verify(builder).sendTracked();
    }

    @Test
    @DisplayName("Uses toPhone for SMS channel")
    void usesToPhoneForSms() {
        QueuedNotification n = QueuedNotification.builder()
                .recipient("+5548999999999")
                .channelName("sms")
                .rawContent("Hello SMS")
                .build();

        consumer.onMessage(n);

        verify(hub).toPhone("+5548999999999");
        verify(builder).sendTracked();
    }

    @Test
    @DisplayName("Uses toPhone for WhatsApp channel")
    void usesToPhoneForWhatsApp() {
        QueuedNotification n = QueuedNotification.builder()
                .recipient("+5548999999999")
                .channelName("whatsapp")
                .rawContent("Hello WhatsApp")
                .build();

        consumer.onMessage(n);

        verify(hub).toPhone("+5548999999999");
    }

    @Test
    @DisplayName("Sets priority when provided")
    void setsPriority() {
        QueuedNotification n = QueuedNotification.builder()
                .recipient("user@test.com")
                .channelName("email")
                .rawContent("Urgent!")
                .priority("URGENT")
                .build();

        consumer.onMessage(n);

        verify(builder).priority(any());
        verify(builder).sendTracked();
    }

    @Test
    @DisplayName("Rethrows exception on send failure")
    void rethrowsOnFailure() {
        doThrow(new RuntimeException("Send failed")).when(builder).sendTracked();

        QueuedNotification n = QueuedNotification.builder()
                .recipient("user@test.com")
                .channelName("email")
                .rawContent("Will fail")
                .build();

        assertThrows(RuntimeException.class, () -> consumer.onMessage(n));
    }
}
