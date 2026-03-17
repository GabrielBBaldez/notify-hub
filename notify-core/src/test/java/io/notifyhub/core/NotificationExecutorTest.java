package io.notifyhub.core;

import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
import io.notifyhub.core.dedup.InMemoryDeduplicationStore;
import io.notifyhub.core.dlq.InMemoryDeadLetterQueue;
import io.notifyhub.core.event.EventType;
import io.notifyhub.core.event.NotificationEvent;
import io.notifyhub.core.event.NotificationEventBus;
import io.notifyhub.core.event.NotificationEventListener;
import io.notifyhub.core.ratelimit.RateLimitExceededException;
import io.notifyhub.core.ratelimit.RateLimiter;
import io.notifyhub.core.retry.RetryPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationExecutorTest {

    @Mock
    private NotificationChannel emailChannel;

    @Mock
    private NotificationChannel smsChannel;

    private final List<NotificationEvent> capturedEvents = new ArrayList<>();
    private NotificationEventBus eventBus;
    private Map<String, NotificationChannel> channelMap;
    private NotifyHub hub;

    @BeforeEach
    void setUp() {
        when(emailChannel.getName()).thenReturn("email");
        when(smsChannel.getName()).thenReturn("sms");
        when(emailChannel.sendWithResult(any())).thenCallRealMethod();
        when(smsChannel.sendWithResult(any())).thenCallRealMethod();

        capturedEvents.clear();
        NotificationEventListener capturingListener = capturedEvents::add;
        eventBus = new NotificationEventBus(List.of(capturingListener));

        channelMap = new ConcurrentHashMap<>();
        channelMap.put("email", emailChannel);
        channelMap.put("sms", smsChannel);

        // Build a minimal hub just to create NotificationBuilder instances
        hub = NotifyHub.builder()
                .channel(emailChannel)
                .channel(smsChannel)
                .build();
    }

    private NotificationExecutor buildExecutor() {
        return buildExecutor(null, null, null, null);
    }

    private NotificationExecutor buildExecutor(
            InMemoryDeduplicationStore dedupStore,
            RateLimiter rateLimiter,
            InMemoryDeadLetterQueue dlq,
            NotificationTracker tracker
    ) {
        return new NotificationExecutor(
                channelMap,
                null, // templateEngine
                RetryPolicy.none(),
                eventBus,
                dedupStore,
                rateLimiter,
                dlq,
                tracker,
                null // asyncExecutor
        );
    }

    // ===================== TESTS =====================

    @Test
    @DisplayName("Should send to channel and publish SENT event")
    void shouldSendToChannelAndPublishSentEvent() {
        NotificationExecutor executor = buildExecutor();

        NotificationBuilder builder = hub.to("user@test.com")
                .via(Channel.EMAIL)
                .content("Hello!");

        executor.execute(builder);

        verify(emailChannel).send(any(Notification.class));
        assertEquals(1, capturedEvents.size());
        NotificationEvent event = capturedEvents.get(0);
        assertEquals(EventType.SENT, event.type());
        assertEquals("email", event.channelName());
    }

    @Test
    @DisplayName("Should try fallback when primary channel fails and publish FAILED then SENT events")
    void shouldTryFallbackWhenPrimaryFails() {
        NotificationExecutor executor = buildExecutor();

        // Reset email channel mock to throw on both send methods
        reset(emailChannel);
        when(emailChannel.getName()).thenReturn("email");
        doThrow(new NotificationSendException("email", "SMTP down"))
                .when(emailChannel).send(any());
        doThrow(new NotificationSendException("email", "SMTP down"))
                .when(emailChannel).sendWithResult(any());

        // Use a Notifiable that provides both email and phone so SMS fallback can resolve
        Notifiable user = new Notifiable() {
            @Override public String getNotifyEmail() { return "user@test.com"; }
            @Override public String getNotifyPhone() { return "+5511999999999"; }
        };

        NotificationBuilder builder = hub.to(user)
                .via(Channel.EMAIL)
                .fallback(Channel.SMS)
                .content("Hello!");

        executor.execute(builder);

        verify(smsChannel).send(any());

        assertEquals(2, capturedEvents.size());
        assertEquals(EventType.FAILED, capturedEvents.get(0).type());
        assertEquals("email", capturedEvents.get(0).channelName());
        assertEquals(EventType.SENT, capturedEvents.get(1).type());
        assertEquals("sms", capturedEvents.get(1).channelName());
    }

    @Test
    @DisplayName("Should send to DLQ after retry exhaustion")
    void shouldSendToDlqAfterRetryExhaustion() {
        InMemoryDeadLetterQueue dlq = new InMemoryDeadLetterQueue();
        NotificationExecutor executor = new NotificationExecutor(
                channelMap, null,
                RetryPolicy.fixed(3, Duration.ofMillis(1)),
                eventBus, null, null, dlq, null, null
        );

        // Reset to override the thenCallRealMethod stub from setUp
        reset(emailChannel);
        when(emailChannel.getName()).thenReturn("email");
        doThrow(new NotificationSendException("email", "Connection refused"))
                .when(emailChannel).sendWithResult(any());

        NotificationBuilder builder = hub.to("user@test.com")
                .via(Channel.EMAIL)
                .content("Hello!");

        assertThrows(NotificationSendException.class, () -> executor.execute(builder));

        assertEquals(1, dlq.count());
        assertEquals("email", dlq.findAll().get(0).getChannelName());
        assertEquals(3, dlq.findAll().get(0).getAttemptCount());
    }

    @Test
    @DisplayName("Should skip duplicate notifications when dedup is configured")
    void shouldSkipDuplicateNotifications() {
        InMemoryDeduplicationStore dedupStore = new InMemoryDeduplicationStore(Duration.ofHours(1));
        NotificationExecutor executor = buildExecutor(dedupStore, null, null, null);

        NotificationBuilder builder1 = hub.to("user@test.com")
                .via(Channel.EMAIL)
                .content("Hello!");

        // First send should succeed
        executor.execute(builder1);
        verify(emailChannel, times(1)).send(any());

        // Second send with same content should be skipped (dedup)
        NotificationBuilder builder2 = hub.to("user@test.com")
                .via(Channel.EMAIL)
                .content("Hello!");

        executor.execute(builder2);

        // Still only 1 actual send -- second was deduplicated
        verify(emailChannel, times(1)).send(any());
    }

    @Test
    @DisplayName("Should throw RateLimitExceededException when rate limiter rejects")
    void shouldThrowRateLimitExceededWhenRateLimiterRejects() {
        RateLimiter rateLimiter = mock(RateLimiter.class);
        when(rateLimiter.tryAcquire("email")).thenReturn(false);

        NotificationExecutor executor = buildExecutor(null, rateLimiter, null, null);

        NotificationBuilder builder = hub.to("user@test.com")
                .via(Channel.EMAIL)
                .content("Hello!");

        RateLimitExceededException ex = assertThrows(
                RateLimitExceededException.class,
                () -> executor.execute(builder)
        );
        assertEquals("email", ex.getChannelName());

        // Channel should never be called
        verify(emailChannel, never()).send(any());
        verify(emailChannel, never()).sendWithResult(any());
    }

    @Test
    @DisplayName("Should allow URGENT priority to bypass rate limiter")
    void shouldBypassRateLimiterForUrgentPriority() {
        RateLimiter rateLimiter = mock(RateLimiter.class);
        when(rateLimiter.tryAcquire("email")).thenReturn(false);

        NotificationExecutor executor = buildExecutor(null, rateLimiter, null, null);

        NotificationBuilder builder = hub.to("user@test.com")
                .via(Channel.EMAIL)
                .content("Urgent message!")
                .priority(Priority.URGENT);

        // Should NOT throw RateLimitExceededException because URGENT bypasses
        executor.execute(builder);

        verify(emailChannel).send(any());
        verify(rateLimiter, never()).tryAcquire(anyString());
    }

    @Test
    @DisplayName("Should record delivery receipt when tracker is configured")
    void shouldRecordDeliveryReceiptWhenTrackerConfigured() {
        NotificationTracker tracker = mock(NotificationTracker.class);
        NotificationExecutor executor = buildExecutor(null, null, null, tracker);

        NotificationBuilder builder = hub.to("user@test.com")
                .via(Channel.EMAIL)
                .content("Tracked message");

        DeliveryReceipt receipt = executor.executeTracked(builder);

        assertNotNull(receipt);
        assertEquals(DeliveryStatus.SENT, receipt.getStatus());
        assertEquals("email", receipt.getChannelName());
        assertEquals("user@test.com", receipt.getRecipient());

        ArgumentCaptor<DeliveryReceipt> captor = ArgumentCaptor.forClass(DeliveryReceipt.class);
        verify(tracker).record(captor.capture());
        assertEquals(DeliveryStatus.SENT, captor.getValue().getStatus());
    }
}
