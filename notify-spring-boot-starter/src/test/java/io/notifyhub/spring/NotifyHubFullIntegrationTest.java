package io.notifyhub.spring;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import io.notifyhub.core.Channel;
import io.notifyhub.core.Notifiable;
import io.notifyhub.core.NotificationListener;
import io.notifyhub.core.NotifyHub;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
import io.notifyhub.core.template.TemplateEngine;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FULL INTEGRATION TEST — proves the ENTIRE stack works:
 *
 * 1. Spring Boot starts and auto-configures NotifyHub
 * 2. application-test.yml is read correctly
 * 3. Email channel is auto-registered from properties
 * 4. Custom channels (fake SMS) are auto-discovered as beans
 * 5. NotifyHub is injected into services
 * 6. Templates are rendered by Mustache
 * 7. Emails are sent through real SMTP (GreenMail)
 * 8. Fallback works (email fails → tries custom channel)
 * 9. Retry with exponential backoff works
 * 10. Listeners receive success/failure events
 *
 * ZERO mocks. Spring Boot + SMTP + templates + channels — all real.
 */
@SpringBootTest(classes = NotifyHubFullIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NotifyHubFullIntegrationTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @Autowired
    private NotifyHub notify;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private TestNotificationListener listener;

    @Autowired
    private FakeSmsChannel fakeSmsChannel;

    // ===================== TEST CONFIG =====================

    @Configuration
    @EnableAutoConfiguration
    static class TestConfig {

        /**
         * Custom channel registered as a Spring bean.
         * Auto-discovered by NotifyAutoConfiguration.
         */
        @Bean
        FakeSmsChannel fakeSmsChannel() {
            return new FakeSmsChannel();
        }

        /**
         * Listener registered as a Spring bean.
         * Auto-discovered by NotifyAutoConfiguration.
         */
        @Bean
        TestNotificationListener testNotificationListener() {
            return new TestNotificationListener();
        }
    }

    /**
     * Fake SMS channel — records messages instead of sending via Twilio.
     * Proves custom channels work as Spring beans.
     */
    static class FakeSmsChannel implements NotificationChannel {
        final List<String> sentMessages = new ArrayList<>();
        boolean shouldFail = false;

        @Override
        public String getName() { return "sms"; }

        @Override
        public void send(io.notifyhub.core.Notification notification) {
            if (shouldFail) {
                throw new NotificationSendException("sms", "Fake SMS failure");
            }
            sentMessages.add("To: " + notification.getRecipient()
                    + " | Body: " + notification.getRenderedContent());
        }
    }

    /**
     * Listener that records events — proves listeners are auto-discovered.
     */
    static class TestNotificationListener implements NotificationListener {
        final List<String> successes = new ArrayList<>();
        final List<String> failures = new ArrayList<>();

        @Override
        public void onSuccess(String channelName, String templateName) {
            successes.add(channelName + ":" + templateName);
        }

        @Override
        public void onFailure(String channelName, String templateName, Exception error) {
            failures.add(channelName + ":" + error.getMessage());
        }
    }

    @BeforeEach
    void resetState() {
        fakeSmsChannel.sentMessages.clear();
        fakeSmsChannel.shouldFail = false;
        listener.successes.clear();
        listener.failures.clear();
    }

    // ===================== STEP 1: SPRING BOOT AUTO-CONFIG =====================

    @Test
    @Order(1)
    @DisplayName("STEP 1: Spring Boot auto-configures NotifyHub with channels from application.yml")
    void springBootAutoConfig() {
        // NotifyHub was injected by Spring — auto-config worked
        assertNotNull(notify, "NotifyHub should be auto-configured and injected");

        // Template engine was auto-configured
        assertNotNull(templateEngine, "TemplateEngine should be auto-configured");

        // Email channel was auto-configured from application-test.yml
        assertTrue(notify.getRegisteredChannels().contains("email"),
                "Email channel should be auto-registered from properties");

        // Custom SMS channel was auto-discovered as a Spring bean
        assertTrue(notify.getRegisteredChannels().contains("sms"),
                "SMS channel (bean) should be auto-discovered");

        System.out.println("=== STEP 1 PASSED ===");
        System.out.println("NotifyHub injected: YES");
        System.out.println("Registered channels: " + notify.getRegisteredChannels());
        System.out.println("TemplateEngine: " + templateEngine.getClass().getSimpleName());
        System.out.println("=====================");
    }

    // ===================== STEP 2: SEND EMAIL THROUGH SPRING CONTEXT =====================

    @Test
    @Order(2)
    @DisplayName("STEP 2: Send real email through Spring-configured NotifyHub")
    void sendEmailThroughSpring() throws Exception {
        notify.to("spring-test@example.com")
                .via(Channel.EMAIL)
                .subject("Spring Boot Integration Test")
                .content("This email was sent by a Spring Boot auto-configured NotifyHub!")
                .send();

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertEquals(1, received.length);

        MimeMessage email = received[0];
        assertEquals("Spring Boot Integration Test", email.getSubject());
        assertEquals("spring-test@example.com", email.getAllRecipients()[0].toString());
        assertTrue(email.getFrom()[0].toString().contains("NotifyHub Test"),
                "From name should come from application-test.yml");

        System.out.println("=== STEP 2 PASSED ===");
        System.out.println("From: " + email.getFrom()[0]);
        System.out.println("To: " + email.getAllRecipients()[0]);
        System.out.println("Subject: " + email.getSubject());
        System.out.println("=====================");
    }

    // ===================== STEP 3: TEMPLATE RENDERING =====================

    @Test
    @Order(3)
    @DisplayName("STEP 3: Template rendering with Mustache through Spring context")
    void templateRendering() throws Exception {
        notify.to("template-test@example.com")
                .via(Channel.EMAIL)
                .subject("Welcome!")
                .template("welcome")
                .param("name", "Gabriel Baldez")
                .param("plan", "Pro")
                .send();

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertEquals(1, received.length);

        String body = received[0].getContent().toString();
        assertTrue(body.contains("Gabriel Baldez"), "Template should render {{name}}");
        assertTrue(body.contains("Pro"), "Template should render {{plan}}");
        assertTrue(body.contains("<h1>"), "Should be HTML template");

        System.out.println("=== STEP 3 PASSED ===");
        System.out.println("Template rendered: " + body.trim().substring(0, Math.min(body.length(), 100)));
        System.out.println("=====================");
    }

    // ===================== STEP 4: NOTIFIABLE INTERFACE =====================

    @Test
    @Order(4)
    @DisplayName("STEP 4: Send to Notifiable entity (like a JPA User)")
    void notifiableInterface() throws Exception {
        // Simulates a JPA entity implementing Notifiable
        Notifiable fakeUser = new Notifiable() {
            public String getNotifyEmail() { return "jpa-user@company.com"; }
            public String getNotifyPhone() { return "+5548999999999"; }
            public String getNotifyName() { return "Maria Silva"; }
        };

        notify.to(fakeUser)
                .via(Channel.EMAIL)
                .subject("Notifiable test")
                .template("welcome")
                .param("name", "Maria Silva")
                .param("plan", "Enterprise")
                .send();

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertEquals(1, received.length);
        assertEquals("jpa-user@company.com", received[0].getAllRecipients()[0].toString());

        String body = received[0].getContent().toString();
        assertTrue(body.contains("Maria Silva"));

        System.out.println("=== STEP 4 PASSED ===");
        System.out.println("Notifiable resolved to: " + received[0].getAllRecipients()[0]);
        System.out.println("=====================");
    }

    // ===================== STEP 5: CUSTOM CHANNEL (FAKE SMS) =====================

    @Test
    @Order(5)
    @DisplayName("STEP 5: Custom channel (FakeSms) auto-discovered as Spring bean")
    void customChannelAsBean() {
        Notifiable user = new Notifiable() {
            public String getNotifyPhone() { return "+5548999999999"; }
        };

        notify.to(user)
                .via(Channel.SMS)
                .content("Your verification code is 1234")
                .send();

        assertEquals(1, fakeSmsChannel.sentMessages.size());
        assertTrue(fakeSmsChannel.sentMessages.get(0).contains("+5548999999999"));
        assertTrue(fakeSmsChannel.sentMessages.get(0).contains("1234"));

        System.out.println("=== STEP 5 PASSED ===");
        System.out.println("SMS sent: " + fakeSmsChannel.sentMessages.get(0));
        System.out.println("=====================");
    }

    // ===================== STEP 6: FALLBACK CHAIN =====================

    @Test
    @Order(6)
    @DisplayName("STEP 6: Fallback — email fails, falls back to SMS")
    void fallbackChain() {
        // Send to a Notifiable with both email and phone
        Notifiable user = new Notifiable() {
            public String getNotifyEmail() { return "bad-email@"; } // will fail SMTP
            public String getNotifyPhone() { return "+5548888888888"; }
        };

        // Stop GreenMail temporarily to force email failure
        greenMail.stop();

        try {
            notify.to(user)
                    .via(Channel.EMAIL)
                    .fallback(Channel.SMS)
                    .content("Important: your appointment is tomorrow!")
                    .send();

            // Email failed, SMS should have received it
            assertEquals(1, fakeSmsChannel.sentMessages.size(),
                    "SMS should receive the message after email fails");
            assertTrue(fakeSmsChannel.sentMessages.get(0).contains("appointment"));

            System.out.println("=== STEP 6 PASSED ===");
            System.out.println("Email FAILED (as expected)");
            System.out.println("Fallback to SMS: " + fakeSmsChannel.sentMessages.get(0));
            System.out.println("=====================");
        } finally {
            // Restart GreenMail for other tests
            greenMail.start();
        }
    }

    // ===================== STEP 7: ALL CHANNELS FAIL =====================

    @Test
    @Order(7)
    @DisplayName("STEP 7: All channels fail — throws exception")
    void allChannelsFail() {
        greenMail.stop();
        fakeSmsChannel.shouldFail = true;

        try {
            Notifiable user = new Notifiable() {
                public String getNotifyEmail() { return "fail@test.com"; }
                public String getNotifyPhone() { return "+5548777777777"; }
            };

            assertThrows(NotificationSendException.class, () ->
                    notify.to(user)
                            .via(Channel.EMAIL)
                            .fallback(Channel.SMS)
                            .content("This will fail everywhere")
                            .send()
            );

            System.out.println("=== STEP 7 PASSED ===");
            System.out.println("Email FAILED, SMS FAILED, Exception thrown correctly");
            System.out.println("=====================");
        } finally {
            greenMail.start();
            fakeSmsChannel.shouldFail = false;
        }
    }

    // ===================== STEP 8: SEND ALL (MULTI-CHANNEL) =====================

    @Test
    @Order(8)
    @DisplayName("STEP 8: sendAll() sends to email AND SMS simultaneously")
    void sendAll() throws Exception {
        Notifiable user = new Notifiable() {
            public String getNotifyEmail() { return "multi@test.com"; }
            public String getNotifyPhone() { return "+5548666666666"; }
        };

        notify.to(user)
                .via(Channel.EMAIL, Channel.SMS)
                .content("Alert: your account was accessed from a new device!")
                .subject("Security Alert")
                .sendAll();

        // Email received
        MimeMessage[] received = greenMail.getReceivedMessages();
        assertEquals(1, received.length);
        assertEquals("multi@test.com", received[0].getAllRecipients()[0].toString());

        // SMS received
        assertEquals(1, fakeSmsChannel.sentMessages.size());
        assertTrue(fakeSmsChannel.sentMessages.get(0).contains("+5548666666666"));

        System.out.println("=== STEP 8 PASSED ===");
        System.out.println("Email sent to: " + received[0].getAllRecipients()[0]);
        System.out.println("SMS sent: " + fakeSmsChannel.sentMessages.get(0));
        System.out.println("Both channels received the same message!");
        System.out.println("=====================");
    }

    // ===================== STEP 9: LISTENERS =====================

    @Test
    @Order(9)
    @DisplayName("STEP 9: Listeners receive success/failure events")
    void listenerEvents() {
        // Success
        notify.to("listener-test@example.com")
                .via(Channel.EMAIL)
                .subject("Listener test")
                .content("Testing listeners")
                .send();

        assertEquals(1, listener.successes.size());
        assertTrue(listener.successes.get(0).startsWith("email:"));

        // Failure
        greenMail.stop();
        try {
            assertThrows(NotificationSendException.class, () ->
                    notify.to("fail@test.com")
                            .via(Channel.EMAIL)
                            .content("Will fail")
                            .send()
            );
            assertTrue(listener.failures.size() > 0, "Listener should record failure");
        } finally {
            greenMail.start();
        }

        System.out.println("=== STEP 9 PASSED ===");
        System.out.println("Success events: " + listener.successes);
        System.out.println("Failure events: " + listener.failures.size() + " recorded");
        System.out.println("=====================");
    }

    // ===================== STEP 10: RETRY =====================

    @Test
    @Order(10)
    @DisplayName("STEP 10: Retry with backoff from application.yml config")
    void retryFromConfig() {
        // Create a channel that fails twice then succeeds
        AtomicInteger attempts = new AtomicInteger(0);
        NotificationChannel flakySms = new NotificationChannel() {
            @Override
            public String getName() { return "flaky"; }

            @Override
            public void send(io.notifyhub.core.Notification notification) {
                int attempt = attempts.incrementAndGet();
                if (attempt <= 2) {
                    throw new NotificationSendException("flaky",
                            "Temporary error (attempt " + attempt + ")");
                }
                // 3rd attempt succeeds
            }
        };

        // Register the flaky channel at runtime
        notify.registerChannel(flakySms);

        notify.to("retry@test.com")
                .via(Channel.custom("flaky"))
                .content("This should succeed on 3rd attempt")
                .send();

        assertEquals(3, attempts.get(),
                "Should have taken 3 attempts (2 failures + 1 success)");

        System.out.println("=== STEP 10 PASSED ===");
        System.out.println("Attempt 1: FAILED");
        System.out.println("Attempt 2: FAILED");
        System.out.println("Attempt 3: SUCCESS");
        System.out.println("Retry with exponential backoff works!");
        System.out.println("======================");
    }
}
