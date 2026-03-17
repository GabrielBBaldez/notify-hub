package io.notifyhub.core.pipeline;

import io.notifyhub.core.Notification;
import io.notifyhub.core.NotificationBuilder;
import io.notifyhub.core.Priority;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
import io.notifyhub.core.channel.SendResult;
import io.notifyhub.core.dedup.InMemoryDeduplicationStore;
import io.notifyhub.core.dlq.DeadLetterQueue;
import io.notifyhub.core.dlq.InMemoryDeadLetterQueue;
import io.notifyhub.core.event.EventType;
import io.notifyhub.core.event.NotificationEvent;
import io.notifyhub.core.event.NotificationEventBus;
import io.notifyhub.core.event.NotificationEventListener;
import io.notifyhub.core.ratelimit.RateLimitExceededException;
import io.notifyhub.core.ratelimit.RateLimiter;
import io.notifyhub.core.retry.RetryPolicy;
import io.notifyhub.core.template.TemplateEngine;
import io.notifyhub.core.NotifyHub;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PipelineTest {

    @Mock
    private NotificationChannel channel;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private RateLimiter rateLimiter;

    private NotifyHub hub;

    @BeforeEach
    void setUp() {
        when(channel.getName()).thenReturn("email");
        // Default: sendWithResult returns null (success, no provider message ID)
        when(channel.sendWithResult(any())).thenReturn(null);
        when(templateEngine.exists(anyString(), anyString())).thenReturn(true);
        when(templateEngine.render(anyString(), anyString(), any(), any())).thenReturn("rendered content");
        when(rateLimiter.tryAcquire(anyString())).thenReturn(true);

        hub = NotifyHub.builder()
                .channel(channel)
                .templateEngine(templateEngine)
                .rateLimiter(rateLimiter)
                .build();
    }

    private SendContext buildContext(String channelName, RetryPolicy policy) {
        NotifyHub localHub = NotifyHub.builder().channel(channel).build();
        NotificationBuilder builder = localHub.to("test@example.com")
                .via(io.notifyhub.core.Channel.EMAIL)
                .content("Hello world");
        return new SendContext(channelName, channel, builder, policy);
    }

    @Test
    @DisplayName("Full pipeline sends successfully: dedup -> ratelimit -> template -> send")
    void testFullPipelineSendsSuccessfully() {
        InMemoryDeduplicationStore deduplicationStore = new InMemoryDeduplicationStore(Duration.ofHours(1));
        InMemoryDeadLetterQueue dlq = new InMemoryDeadLetterQueue();
        List<NotificationEvent> publishedEvents = new ArrayList<>();
        NotificationEventBus eventBus = new NotificationEventBus(
                List.of((NotificationEventListener) publishedEvents::add)
        );

        when(rateLimiter.tryAcquire("email")).thenReturn(true);
        when(templateEngine.render(anyString(), anyString(), any(), any())).thenReturn("rendered");

        SendPipeline pipeline = SendPipeline.build(
                deduplicationStore, rateLimiter, templateEngine, dlq, eventBus);

        NotifyHub localHub = NotifyHub.builder().channel(channel).build();
        NotificationBuilder builder = localHub.to("user@example.com")
                .via(io.notifyhub.core.Channel.EMAIL)
                .template("welcome")
                .param("name", "Alice");

        SendContext context = new SendContext("email", channel, builder, RetryPolicy.none());
        SendResult result = pipeline.execute(context);

        // channel.sendWithResult() was called
        verify(channel).sendWithResult(any(Notification.class));
        // dedup store should now have one entry
        assertEquals(1, deduplicationStore.size());
        // DLQ should be empty
        assertEquals(0, dlq.count());
    }

    @Test
    @DisplayName("Deduplication skips duplicate notification and returns null")
    void testDeduplicationSkipsDuplicate() {
        InMemoryDeduplicationStore deduplicationStore = new InMemoryDeduplicationStore(Duration.ofHours(1));
        List<NotificationEvent> publishedEvents = new ArrayList<>();
        NotificationEventBus eventBus = new NotificationEventBus(
                List.of((NotificationEventListener) publishedEvents::add)
        );

        SendPipeline pipeline = SendPipeline.build(
                deduplicationStore, null, templateEngine, null, eventBus);

        NotifyHub localHub = NotifyHub.builder().channel(channel).build();
        NotificationBuilder builder = localHub.to("dup@example.com")
                .via(io.notifyhub.core.Channel.EMAIL)
                .deduplicationKey("unique-key-123")
                .content("Duplicate notification");

        SendContext context = new SendContext("email", channel, builder, RetryPolicy.none());

        // First send — should pass through
        pipeline.execute(context);
        verify(channel, times(1)).sendWithResult(any(Notification.class));

        // Second send with same context/key — should be skipped
        SendContext context2 = new SendContext("email", channel, builder, RetryPolicy.none());
        SendResult result = pipeline.execute(context2);

        assertNull(result, "Duplicate notification should return null");
        // channel.sendWithResult() should NOT have been called again
        verify(channel, times(1)).sendWithResult(any(Notification.class));

        // A DEDUPED event should have been published
        boolean dedupedEventPublished = publishedEvents.stream()
                .anyMatch(e -> e.type() == EventType.DEDUPED);
        assertTrue(dedupedEventPublished, "Expected DEDUPED event to be published");
    }

    @Test
    @DisplayName("Rate limiter rejects notification when limit exceeded and throws RateLimitExceededException")
    void testRateLimiterRejectsWhenLimitExceeded() {
        when(rateLimiter.tryAcquire("email")).thenReturn(false);

        List<NotificationEvent> publishedEvents = new ArrayList<>();
        NotificationEventBus eventBus = new NotificationEventBus(
                List.of((NotificationEventListener) publishedEvents::add)
        );

        SendPipeline pipeline = SendPipeline.build(
                null, rateLimiter, templateEngine, null, eventBus);

        NotifyHub localHub = NotifyHub.builder().channel(channel).build();
        NotificationBuilder builder = localHub.to("user@example.com")
                .via(io.notifyhub.core.Channel.EMAIL)
                .content("Rate limited notification");

        SendContext context = new SendContext("email", channel, builder, RetryPolicy.none());

        assertThrows(RateLimitExceededException.class, () -> pipeline.execute(context));

        // channel.sendWithResult() should NOT have been called
        verify(channel, never()).sendWithResult(any(Notification.class));

        // RATE_LIMITED event should have been published
        boolean rateLimitedEventPublished = publishedEvents.stream()
                .anyMatch(e -> e.type() == EventType.RATE_LIMITED);
        assertTrue(rateLimitedEventPublished, "Expected RATE_LIMITED event to be published");
    }

    @Test
    @DisplayName("URGENT priority bypasses rate limiter")
    void testUrgentPriorityBypassesRateLimit() {
        when(rateLimiter.tryAcquire("email")).thenReturn(false);

        SendPipeline pipeline = SendPipeline.build(
                null, rateLimiter, templateEngine, null, null);

        NotifyHub localHub = NotifyHub.builder().channel(channel).build();
        NotificationBuilder builder = localHub.to("user@example.com")
                .via(io.notifyhub.core.Channel.EMAIL)
                .priority(Priority.URGENT)
                .content("Urgent notification");

        SendContext context = new SendContext("email", channel, builder, RetryPolicy.none());

        // Should NOT throw — URGENT bypasses rate limiting
        assertDoesNotThrow(() -> pipeline.execute(context));
        verify(channel).sendWithResult(any(Notification.class));
    }

    @Test
    @DisplayName("Retry handler retries on failure according to retry policy")
    void testRetryHandlerRetriesOnFailure() {
        // Fail first 2 attempts, succeed on 3rd — use doThrow/doReturn for clean override
        doThrow(new NotificationSendException("email", "Transient error"))
                .doThrow(new NotificationSendException("email", "Transient error"))
                .doReturn(null)
                .when(channel).sendWithResult(any());

        List<NotificationEvent> publishedEvents = new ArrayList<>();
        NotificationEventBus eventBus = new NotificationEventBus(
                List.of((NotificationEventListener) publishedEvents::add)
        );

        SendPipeline pipeline = SendPipeline.build(
                null, null, templateEngine, null, eventBus);

        NotifyHub localHub = NotifyHub.builder().channel(channel).build();
        NotificationBuilder builder = localHub.to("user@example.com")
                .via(io.notifyhub.core.Channel.EMAIL)
                .content("Retried notification");

        // Use fixed retry with zero delay for test speed
        RetryPolicy policy = RetryPolicy.fixed(3, Duration.ZERO);
        SendContext context = new SendContext("email", channel, builder, policy);

        // Should succeed on 3rd attempt
        assertDoesNotThrow(() -> pipeline.execute(context));

        // channel.sendWithResult() should be called 3 times
        verify(channel, times(3)).sendWithResult(any(Notification.class));

        // Two RETRIED events should be published
        long retriedEvents = publishedEvents.stream()
                .filter(e -> e.type() == EventType.RETRIED)
                .count();
        assertEquals(2, retriedEvents, "Expected 2 RETRIED events (attempts 2 and 3)");
    }

    @Test
    @DisplayName("DLQ receives notification after all retries are exhausted")
    void testDlqReceivesNotificationAfterRetryExhaustion() {
        doThrow(new NotificationSendException("email", "Persistent error"))
                .when(channel).sendWithResult(any());

        InMemoryDeadLetterQueue dlq = new InMemoryDeadLetterQueue();
        List<NotificationEvent> publishedEvents = new ArrayList<>();
        NotificationEventBus eventBus = new NotificationEventBus(
                List.of((NotificationEventListener) publishedEvents::add)
        );

        SendPipeline pipeline = SendPipeline.build(
                null, null, templateEngine, dlq, eventBus);

        NotifyHub localHub = NotifyHub.builder().channel(channel).build();
        NotificationBuilder builder = localHub.to("fail@example.com")
                .via(io.notifyhub.core.Channel.EMAIL)
                .content("This will fail");

        RetryPolicy policy = RetryPolicy.fixed(2, Duration.ZERO);
        SendContext context = new SendContext("email", channel, builder, policy);

        assertThrows(NotificationSendException.class, () -> pipeline.execute(context));

        // DLQ should have one entry
        assertEquals(1, dlq.count(), "Expected one dead letter after retry exhaustion");
        assertEquals("email", dlq.findAll().get(0).getChannelName());
        assertEquals("fail@example.com", dlq.findAll().get(0).getRecipient());

        // A FAILED event should have been published
        boolean failedEventPublished = publishedEvents.stream()
                .anyMatch(e -> e.type() == EventType.FAILED);
        assertTrue(failedEventPublished, "Expected FAILED event to be published");

        // channel.sendWithResult() should have been called exactly 2 times (maxAttempts)
        verify(channel, times(2)).sendWithResult(any(Notification.class));
    }
}
