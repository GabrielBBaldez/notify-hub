package io.notifyhub.core.event;

import io.notifyhub.core.NotificationListener;
import io.notifyhub.core.channel.NotificationSendException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LegacyListenerAdapterTest {

    @Mock
    private NotificationListener legacy;

    private LegacyListenerAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new LegacyListenerAdapter(legacy);
    }

    @Test
    @DisplayName("SENT event maps to onSuccess with channelName and templateName")
    void sentMapsToOnSuccess() {
        NotificationEvent event = NotificationEvent.builder(EventType.SENT, "email")
                .templateName("welcome")
                .build();

        adapter.onEvent(event);

        verify(legacy).onSuccess("email", "welcome");
    }

    @Test
    @DisplayName("FAILED event maps to onFailure with NotificationSendException")
    void failedMapsToOnFailure() {
        NotificationEvent event = NotificationEvent.builder(EventType.FAILED, "slack")
                .templateName("alert")
                .errorMessage("Connection refused")
                .build();

        adapter.onEvent(event);

        verify(legacy).onFailure(eq("slack"), eq("alert"), any(NotificationSendException.class));
    }

    @Test
    @DisplayName("SCHEDULED event maps to onScheduled with channelName, recipient, and latency")
    void scheduledMapsToOnScheduled() {
        Duration delay = Duration.ofMinutes(5);
        NotificationEvent event = NotificationEvent.builder(EventType.SCHEDULED, "telegram")
                .recipient("user@example.com")
                .latency(delay)
                .build();

        adapter.onEvent(event);

        verify(legacy).onScheduled("telegram", "user@example.com", delay);
    }

    @Test
    @DisplayName("CANCELLED event maps to onCancelled with channelName and recipient")
    void cancelledMapsToOnCancelled() {
        NotificationEvent event = NotificationEvent.builder(EventType.CANCELLED, "discord")
                .recipient("user@example.com")
                .build();

        adapter.onEvent(event);

        verify(legacy).onCancelled("discord", "user@example.com");
    }
}
