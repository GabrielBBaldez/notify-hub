package io.notifyhub.core;

import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
import io.notifyhub.core.event.NotificationEventBus;
import io.notifyhub.core.retry.RetryPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChannelSuggestionTest {

    @Mock private NotificationChannel slackChannel;
    @Mock private NotificationChannel emailChannel;

    private NotificationExecutor executor;
    private NotifyHub hub;

    @BeforeEach
    void setUp() {
        when(slackChannel.getName()).thenReturn("slack");
        when(emailChannel.getName()).thenReturn("email");

        Map<String, NotificationChannel> channels = new ConcurrentHashMap<>();
        channels.put("slack", slackChannel);
        channels.put("email", emailChannel);

        NotificationEventBus eventBus = new NotificationEventBus(List.of());
        executor = new NotificationExecutor(channels, null, RetryPolicy.none(), eventBus,
                null, null, null, null, null);

        hub = NotifyHub.builder().channel(slackChannel).channel(emailChannel).build();
    }

    @Test
    @DisplayName("Should suggest closest channel name on typo")
    void suggestClosestChannel() {
        NotificationBuilder builder = new NotificationBuilder(hub)
                .to("user@test.com").via(Channel.SLACK).content("Hello");

        NotificationSendException ex = assertThrows(NotificationSendException.class,
                () -> executor.sendToChannel("slck", builder));

        assertTrue(ex.getMessage().contains("Did you mean 'slack'?"),
                "Expected suggestion 'slack' in: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should not suggest when no close match")
    void noSuggestionWhenNoCloseMatch() {
        NotificationBuilder builder = new NotificationBuilder(hub)
                .to("user@test.com").via(Channel.SLACK).content("Hello");

        NotificationSendException ex = assertThrows(NotificationSendException.class,
                () -> executor.sendToChannel("unknown-channel", builder));

        assertFalse(ex.getMessage().contains("Did you mean"),
                "Expected no suggestion in: " + ex.getMessage());
    }
}
