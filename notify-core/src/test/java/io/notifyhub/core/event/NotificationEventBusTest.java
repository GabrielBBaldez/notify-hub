package io.notifyhub.core.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotificationEventBusTest {

    @Test
    @DisplayName("Should publish event to all registered listeners")
    void shouldPublishToAllListeners() {
        List<NotificationEvent> received1 = new ArrayList<>();
        List<NotificationEvent> received2 = new ArrayList<>();

        NotificationEventBus bus = new NotificationEventBus(List.of(
                received1::add,
                received2::add
        ));

        NotificationEvent event = NotificationEvent.builder(EventType.SENT, "email")
                .recipient("user@example.com")
                .build();

        bus.publish(event);

        assertEquals(1, received1.size());
        assertEquals(1, received2.size());
        assertSame(event, received1.get(0));
        assertSame(event, received2.get(0));
    }

    @Test
    @DisplayName("Should continue delivering to remaining listeners when one throws an exception")
    void shouldContinueWhenListenerThrows() {
        List<NotificationEvent> received = new ArrayList<>();

        NotificationEventBus bus = new NotificationEventBus(List.of(
                e -> { throw new RuntimeException("boom"); },
                received::add
        ));

        NotificationEvent event = NotificationEvent.builder(EventType.FAILED, "slack")
                .errorMessage("connection refused")
                .build();

        assertDoesNotThrow(() -> bus.publish(event));
        assertEquals(1, received.size());
        assertSame(event, received.get(0));
    }

    @Test
    @DisplayName("Should work with an empty listener list")
    void shouldWorkWithEmptyListenerList() {
        NotificationEventBus bus = new NotificationEventBus(List.of());

        NotificationEvent event = NotificationEvent.builder(EventType.RATE_LIMITED, "telegram")
                .build();

        assertDoesNotThrow(() -> bus.publish(event));
    }
}
