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

import java.util.Map;

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
        when(templateEngine.render(eq("welcome"), eq("html"), anyMap()))
                .thenReturn("<h1>Hello Gabriel!</h1>");
        buildHub();

        hub.to("user@test.com")
                .via(Channel.EMAIL)
                .subject("Welcome")
                .template("welcome")
                .param("name", "Gabriel")
                .send();

        verify(templateEngine).render(eq("welcome"), eq("html"), anyMap());
        verify(emailChannel).send(any(Notification.class));
    }

    @Test
    @DisplayName("Should send SMS with text template")
    void sendSmsWithTemplate() {
        when(templateEngine.render(eq("code"), eq("txt"), anyMap()))
                .thenReturn("Your code: 1234");
        buildHub();

        hub.toPhone("+5548999999999")
                .via(Channel.SMS)
                .template("code")
                .param("code", "1234")
                .send();

        verify(templateEngine).render(eq("code"), eq("txt"), anyMap());
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
}
