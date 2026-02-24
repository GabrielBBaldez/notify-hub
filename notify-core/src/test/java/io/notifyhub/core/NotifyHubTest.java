package io.notifyhub.core;

import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
import io.notifyhub.core.retry.RetryPolicy;
import io.notifyhub.core.template.TemplateEngine;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotifyHubTest {

    @Mock
    private NotificationChannel emailChannel;

    @Mock
    private NotificationChannel smsChannel;

    @Mock
    private TemplateEngine templateEngine;

    private NotifyHub hub;

    @BeforeEach
    void setUp() {
        when(emailChannel.getName()).thenReturn("email");
        when(smsChannel.getName()).thenReturn("sms");
    }

    private void buildHub() {
        hub = NotifyHub.builder()
                .channel(emailChannel)
                .channel(smsChannel)
                .templateEngine(templateEngine)
                .build();
    }

    // ===================== BASIC SEND =====================

    @Test
    @DisplayName("Should send email with raw content")
    void sendEmailWithContent() {
        buildHub();

        hub.to("user@test.com")
                .via(Channel.EMAIL)
                .subject("Hello")
                .content("Welcome to our app!")
                .send();

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(emailChannel).send(captor.capture());

        Notification notification = captor.getValue();
        assertEquals("user@test.com", notification.getRecipient());
        assertEquals("email", notification.getChannelName());
        assertEquals("Hello", notification.getSubject());
        assertEquals("Welcome to our app!", notification.getRenderedContent());
    }

    @Test
    @DisplayName("Should send email with template")
    void sendEmailWithTemplate() {
        when(templateEngine.render(eq("welcome"), eq("html"), anyMap(), any()))
                .thenReturn("<h1>Hello Gabriel!</h1>");
        buildHub();

        hub.to("user@test.com")
                .via(Channel.EMAIL)
                .subject("Welcome")
                .template("welcome")
                .param("name", "Gabriel")
                .send();

        verify(templateEngine).render(eq("welcome"), eq("html"), anyMap(), any());
        verify(emailChannel).send(any(Notification.class));
    }

    @Test
    @DisplayName("Should send SMS with text template")
    void sendSmsWithTemplate() {
        when(templateEngine.render(eq("code"), eq("txt"), anyMap(), any()))
                .thenReturn("Your code: 1234");
        buildHub();

        hub.toPhone("+5548999999999")
                .via(Channel.SMS)
                .template("code")
                .param("code", "1234")
                .send();

        verify(templateEngine).render(eq("code"), eq("txt"), anyMap(), any());
        verify(smsChannel).send(any(Notification.class));
    }

    // ===================== NOTIFIABLE =====================

    @Test
    @DisplayName("Should resolve recipient from Notifiable interface")
    void sendToNotifiable() {
        buildHub();

        Notifiable user = new Notifiable() {
            public String getNotifyEmail() { return "gabriel@test.com"; }
            public String getNotifyPhone() { return "+5548999999999"; }
            public String getNotifyName() { return "Gabriel"; }
        };

        hub.to(user)
                .via(Channel.EMAIL)
                .content("Hello!")
                .send();

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(emailChannel).send(captor.capture());
        assertEquals("gabriel@test.com", captor.getValue().getRecipient());
    }

    // ===================== FALLBACK =====================

    @Test
    @DisplayName("Should fallback to SMS when email fails")
    void fallbackToSms() {
        doThrow(new NotificationSendException("email", "SMTP error"))
                .when(emailChannel).send(any());
        buildHub();

        hub.to(new Notifiable() {
                    public String getNotifyEmail() { return "user@test.com"; }
                    public String getNotifyPhone() { return "+5548999"; }
                })
                .via(Channel.EMAIL)
                .fallback(Channel.SMS)
                .content("Important message")
                .send();

        verify(emailChannel).send(any());
        verify(smsChannel).send(any());
    }

    @Test
    @DisplayName("Should throw when all channels fail")
    void allChannelsFail() {
        doThrow(new NotificationSendException("email", "SMTP error"))
                .when(emailChannel).send(any());
        doThrow(new NotificationSendException("sms", "Twilio error"))
                .when(smsChannel).send(any());
        buildHub();

        assertThrows(NotificationSendException.class, () ->
                hub.to(new Notifiable() {
                            public String getNotifyEmail() { return "user@test.com"; }
                            public String getNotifyPhone() { return "+5548999"; }
                        })
                        .via(Channel.EMAIL)
                        .fallback(Channel.SMS)
                        .content("Will fail")
                        .send()
        );
    }

    // ===================== SEND ALL =====================

    @Test
    @DisplayName("Should send through ALL channels with sendAll()")
    void sendAll() {
        buildHub();

        hub.to(new Notifiable() {
                    public String getNotifyEmail() { return "user@test.com"; }
                    public String getNotifyPhone() { return "+5548999"; }
                })
                .via(Channel.EMAIL, Channel.SMS)
                .content("Alert!")
                .sendAll();

        verify(emailChannel).send(any());
        verify(smsChannel).send(any());
    }

    // ===================== RETRY =====================

    @Test
    @DisplayName("Should retry on failure with retry policy")
    void retryOnFailure() {
        doThrow(new NotificationSendException("email", "Temporary error"))
                .doNothing()
                .when(emailChannel).send(any());

        hub = NotifyHub.builder()
                .channel(emailChannel)
                .defaultRetryPolicy(RetryPolicy.fixed(3, java.time.Duration.ofMillis(10)))
                .build();

        hub.to("user@test.com")
                .via(Channel.EMAIL)
                .content("Retry test")
                .send();

        verify(emailChannel, times(2)).send(any());
    }

    // ===================== VALIDATION =====================

    @Test
    @DisplayName("Should throw when no channel specified")
    void noChannelThrows() {
        buildHub();

        assertThrows(IllegalStateException.class, () ->
                hub.to("user@test.com")
                        .content("test")
                        .send()
        );
    }

    @Test
    @DisplayName("Should throw when no content specified")
    void noContentThrows() {
        buildHub();

        assertThrows(IllegalStateException.class, () ->
                hub.to("user@test.com")
                        .via(Channel.EMAIL)
                        .send()
        );
    }

    @Test
    @DisplayName("Should throw when channel not registered")
    void unregisteredChannelThrows() {
        hub = NotifyHub.builder().build(); // no channels

        assertThrows(NotificationSendException.class, () ->
                hub.to("user@test.com")
                        .via(Channel.EMAIL)
                        .content("test")
                        .send()
        );
    }

    // ===================== LISTENERS =====================

    @Test
    @DisplayName("Should notify listeners on success")
    void listenerOnSuccess() {
        NotificationListener listener = mock(NotificationListener.class);
        hub = NotifyHub.builder()
                .channel(emailChannel)
                .listener(listener)
                .build();

        hub.to("user@test.com")
                .via(Channel.EMAIL)
                .content("test")
                .send();

        verify(listener).onSuccess("email", null);
    }

    @Test
    @DisplayName("Should notify listeners on failure")
    void listenerOnFailure() {
        doThrow(new NotificationSendException("email", "fail"))
                .when(emailChannel).send(any());
        NotificationListener listener = mock(NotificationListener.class);
        hub = NotifyHub.builder()
                .channel(emailChannel)
                .listener(listener)
                .build();

        assertThrows(NotificationSendException.class, () ->
                hub.to("user@test.com")
                        .via(Channel.EMAIL)
                        .content("test")
                        .send()
        );

        verify(listener).onFailure(eq("email"), isNull(), any(Exception.class));
    }

    // ===================== CUSTOM CHANNEL =====================

    @Test
    @DisplayName("Should support custom channels registered at runtime")
    void customChannel() {
        NotificationChannel slackChannel = mock(NotificationChannel.class);
        when(slackChannel.getName()).thenReturn("slack");
        buildHub();

        hub.registerChannel(slackChannel);

        hub.to("#general")
                .via(Channel.custom("slack"))
                .content("Deploy complete!")
                .send();

        verify(slackChannel).send(any());
    }

    // ===================== DELIVERY TRACKING =====================

    @Test
    @DisplayName("Should return delivery receipt on sendTracked()")
    void sendTracked() {
        buildHub();

        DeliveryReceipt receipt = hub.to("user@test.com")
                .via(Channel.EMAIL)
                .subject("Hello")
                .content("Tracked notification")
                .sendTracked();

        assertNotNull(receipt);
        assertNotNull(receipt.getId());
        assertEquals("email", receipt.getChannelName());
        assertEquals("user@test.com", receipt.getRecipient());
        assertEquals(DeliveryStatus.SENT, receipt.getStatus());
        assertNotNull(receipt.getTimestamp());
        assertNull(receipt.getErrorMessage());

        verify(emailChannel).send(any());
    }

    @Test
    @DisplayName("Should return FAILED receipt when sendTracked() fails")
    void sendTrackedFails() {
        doThrow(new NotificationSendException("email", "SMTP error"))
                .when(emailChannel).send(any());
        buildHub();

        assertThrows(NotificationSendException.class, () ->
                hub.to("user@test.com")
                        .via(Channel.EMAIL)
                        .content("Will fail")
                        .sendTracked()
        );
    }

    @Test
    @DisplayName("Should record receipt in tracker on sendTracked()")
    void sendTrackedWithTracker() {
        NotificationTracker tracker = new InMemoryNotificationTracker();
        hub = NotifyHub.builder()
                .channel(emailChannel)
                .tracker(tracker)
                .build();

        DeliveryReceipt receipt = hub.to("user@test.com")
                .via(Channel.EMAIL)
                .content("Tracked!")
                .sendTracked();

        assertEquals(1, tracker.count());
        assertTrue(tracker.findById(receipt.getId()).isPresent());
        assertEquals(DeliveryStatus.SENT, tracker.findById(receipt.getId()).get().getStatus());
    }

    @Test
    @DisplayName("Should return receipts for all channels with sendAllTracked()")
    void sendAllTracked() {
        buildHub();

        List<DeliveryReceipt> receipts = hub.to(new Notifiable() {
                    public String getNotifyEmail() { return "user@test.com"; }
                    public String getNotifyPhone() { return "+5548999"; }
                })
                .via(Channel.EMAIL, Channel.SMS)
                .content("Multi-tracked!")
                .sendAllTracked();

        assertEquals(2, receipts.size());
        assertEquals(DeliveryStatus.SENT, receipts.get(0).getStatus());
        assertEquals(DeliveryStatus.SENT, receipts.get(1).getStatus());
        assertEquals("email", receipts.get(0).getChannelName());
        assertEquals("sms", receipts.get(1).getChannelName());
    }

    @Test
    @DisplayName("Should include FAILED receipts in sendAllTracked() partial failures")
    void sendAllTrackedPartialFailure() {
        doThrow(new NotificationSendException("email", "SMTP error"))
                .when(emailChannel).send(any());
        buildHub();

        List<DeliveryReceipt> receipts = hub.to(new Notifiable() {
                    public String getNotifyEmail() { return "user@test.com"; }
                    public String getNotifyPhone() { return "+5548999"; }
                })
                .via(Channel.EMAIL, Channel.SMS)
                .content("Partial fail!")
                .sendAllTracked();

        assertEquals(2, receipts.size());
        assertEquals(DeliveryStatus.FAILED, receipts.get(0).getStatus());
        assertTrue(receipts.get(0).getErrorMessage().contains("SMTP error"));
        assertEquals(DeliveryStatus.SENT, receipts.get(1).getStatus());
    }

    // ===================== IN-MEMORY TRACKER =====================

    @Test
    @DisplayName("InMemoryNotificationTracker should support all query operations")
    void inMemoryTracker() {
        InMemoryNotificationTracker tracker = new InMemoryNotificationTracker();

        DeliveryReceipt sent = DeliveryReceipt.builder()
                .channelName("email")
                .recipient("user@test.com")
                .status(DeliveryStatus.SENT)
                .build();

        DeliveryReceipt failed = DeliveryReceipt.builder()
                .channelName("sms")
                .recipient("user@test.com")
                .status(DeliveryStatus.FAILED)
                .errorMessage("Twilio error")
                .build();

        DeliveryReceipt other = DeliveryReceipt.builder()
                .channelName("email")
                .recipient("other@test.com")
                .status(DeliveryStatus.SENT)
                .build();

        tracker.record(sent);
        tracker.record(failed);
        tracker.record(other);

        assertEquals(3, tracker.count());
        assertEquals(2, tracker.findByRecipient("user@test.com").size());
        assertEquals(1, tracker.findByRecipient("other@test.com").size());
        assertEquals(2, tracker.findByStatus(DeliveryStatus.SENT).size());
        assertEquals(1, tracker.findByStatus(DeliveryStatus.FAILED).size());

        assertTrue(tracker.findById(sent.getId()).isPresent());
        assertTrue(tracker.findById(failed.getId()).isPresent());

        tracker.clear();
        assertEquals(0, tracker.count());
    }

    // ===================== SCHEDULED NOTIFICATIONS =====================

    @Test
    @DisplayName("Should schedule notification with delay")
    void scheduleWithDelay() throws Exception {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        hub = NotifyHub.builder()
                .channel(emailChannel)
                .scheduler(scheduler)
                .build();

        ScheduledNotification scheduled = hub.to("user@test.com")
                .via(Channel.EMAIL)
                .content("Reminder!")
                .schedule(Duration.ofMillis(100));

        assertNotNull(scheduled);
        assertNotNull(scheduled.getId());
        assertEquals("email", scheduled.getChannelName());
        assertEquals("user@test.com", scheduled.getRecipient());
        assertEquals(DeliveryStatus.SCHEDULED, scheduled.getStatus());
        assertFalse(scheduled.isDone());

        // Wait for it to fire
        Thread.sleep(300);

        assertTrue(scheduled.isDone());
        assertEquals(DeliveryStatus.SENT, scheduled.getStatus());
        verify(emailChannel).send(any());

        scheduler.shutdown();
    }

    @Test
    @DisplayName("Should cancel scheduled notification before delivery")
    void cancelScheduled() throws Exception {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        hub = NotifyHub.builder()
                .channel(emailChannel)
                .scheduler(scheduler)
                .build();

        ScheduledNotification scheduled = hub.to("user@test.com")
                .via(Channel.EMAIL)
                .content("Will be cancelled")
                .schedule(Duration.ofSeconds(5));

        assertTrue(scheduled.cancel());
        assertTrue(scheduled.isCancelled());
        assertEquals(DeliveryStatus.CANCELLED, scheduled.getStatus());

        // Wait a bit and verify it never fires
        Thread.sleep(200);
        verify(emailChannel, never()).send(any());

        scheduler.shutdown();
    }

    @Test
    @DisplayName("Should notify listener when notification is scheduled")
    void scheduledListenerNotified() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        NotificationListener listener = mock(NotificationListener.class);
        hub = NotifyHub.builder()
                .channel(emailChannel)
                .scheduler(scheduler)
                .listener(listener)
                .build();

        hub.to("user@test.com")
                .via(Channel.EMAIL)
                .content("Scheduled!")
                .schedule(Duration.ofSeconds(10));

        verify(listener).onScheduled(eq("email"), eq("user@test.com"), any(Duration.class));

        scheduler.shutdownNow();
    }

    @Test
    @DisplayName("Should record scheduled status in tracker")
    void scheduledWithTracker() throws Exception {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        NotificationTracker tracker = new InMemoryNotificationTracker();
        hub = NotifyHub.builder()
                .channel(emailChannel)
                .scheduler(scheduler)
                .tracker(tracker)
                .build();

        ScheduledNotification scheduled = hub.to("user@test.com")
                .via(Channel.EMAIL)
                .content("Tracked schedule!")
                .schedule(Duration.ofMillis(100));

        // Should have SCHEDULED receipt immediately
        assertEquals(1, tracker.count());
        assertEquals(DeliveryStatus.SCHEDULED,
                tracker.findByStatus(DeliveryStatus.SCHEDULED).get(0).getStatus());

        // Wait for delivery
        Thread.sleep(300);

        // Should now have SCHEDULED + SENT receipts
        assertEquals(2, tracker.count());
        assertFalse(tracker.findByStatus(DeliveryStatus.SENT).isEmpty());

        scheduler.shutdown();
    }

    @Test
    @DisplayName("Should throw when scheduling with negative delay")
    void scheduleNegativeDelayThrows() {
        buildHub();

        assertThrows(IllegalArgumentException.class, () ->
                hub.to("user@test.com")
                        .via(Channel.EMAIL)
                        .content("test")
                        .schedule(Duration.ofMinutes(-5))
        );
    }

    @Test
    @DisplayName("Should throw when scheduling in the past with scheduleAt()")
    void scheduleInPastThrows() {
        buildHub();

        assertThrows(IllegalArgumentException.class, () ->
                hub.to("user@test.com")
                        .via(Channel.EMAIL)
                        .content("test")
                        .scheduleAt(LocalDateTime.of(2020, 1, 1, 0, 0))
        );
    }

    @Test
    @DisplayName("Should schedule notification with scheduleAt() for future date")
    void scheduleAtFuture() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        hub = NotifyHub.builder()
                .channel(emailChannel)
                .scheduler(scheduler)
                .build();

        // Schedule for 1 hour from now
        LocalDateTime futureTime = LocalDateTime.now().plusHours(1);

        ScheduledNotification scheduled = hub.to("user@test.com")
                .via(Channel.EMAIL)
                .content("Future delivery!")
                .scheduleAt(futureTime);

        assertNotNull(scheduled);
        assertEquals(DeliveryStatus.SCHEDULED, scheduled.getStatus());
        assertFalse(scheduled.isDone());

        // Remaining delay should be roughly 1 hour
        Duration remaining = scheduled.getRemainingDelay();
        assertTrue(remaining.toMinutes() >= 55); // at least 55 minutes

        scheduled.cancel();
        scheduler.shutdown();
    }
}
