package io.notifyhub.channel.email;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import io.notifyhub.core.Channel;
import io.notifyhub.core.Notifiable;
import io.notifyhub.core.NotifyHub;
import io.notifyhub.core.channel.NotificationSendException;
import io.notifyhub.core.retry.RetryPolicy;
import io.notifyhub.core.template.MustacheTemplateEngine;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * INTEGRATION TEST — uses a real SMTP server (GreenMail in memory).
 *
 * This test proves the full pipeline works:
 * 1. NotifyHub builds the notification
 * 2. MustacheTemplateEngine renders the template
 * 3. SmtpEmailChannel connects to SMTP and sends
 * 4. We read the received email and verify content
 *
 * No mocks. Everything is real.
 */
class SmtpEmailChannelIntegrationTest {

    // Starts a real SMTP server on localhost:3025
    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    private NotifyHub notify;

    @BeforeEach
    void setUp() {
        // Configure channel to connect to GreenMail's SMTP server
        SmtpConfig config = SmtpConfig.builder()
                .host("localhost")
                .port(3025)
                .from("noreply@notifyhub.io")
                .fromName("NotifyHub")
                .tls(false) // GreenMail doesn't need TLS
                .build();

        SmtpEmailChannel emailChannel = new SmtpEmailChannel(config);

        notify = NotifyHub.builder()
                .channel(emailChannel)
                .templateEngine(new MustacheTemplateEngine())
                .build();
    }

    // ===================== REAL EMAIL TESTS =====================

    @Test
    @DisplayName("Should send a real email with raw content and receive it")
    void sendRealEmailWithContent() throws Exception {
        // SEND
        notify.to("gabriel@test.com")
                .via(Channel.EMAIL)
                .subject("Hello from NotifyHub!")
                .content("This is a real email sent through a real SMTP server.")
                .send();

        // RECEIVE — check what the server actually got
        MimeMessage[] received = greenMail.getReceivedMessages();
        assertEquals(1, received.length, "Should have received exactly 1 email");

        MimeMessage email = received[0];
        assertEquals("Hello from NotifyHub!", email.getSubject());
        assertEquals("gabriel@test.com", email.getAllRecipients()[0].toString());
        assertTrue(email.getFrom()[0].toString().contains("noreply@notifyhub.io"));

        String body = email.getContent().toString().trim();
        assertTrue(body.contains("real email sent through a real SMTP server"),
                "Body should contain our message. Got: " + body);

        System.out.println("=== EMAIL RECEIVED ===");
        System.out.println("From: " + email.getFrom()[0]);
        System.out.println("To: " + email.getAllRecipients()[0]);
        System.out.println("Subject: " + email.getSubject());
        System.out.println("Body: " + body);
        System.out.println("======================");
    }

    @Test
    @DisplayName("Should send a real HTML email and receive it")
    void sendRealHtmlEmail() throws Exception {
        String htmlContent = """
                <html>
                <body>
                    <h1>Welcome, Gabriel!</h1>
                    <p>Your account has been created successfully.</p>
                    <p>Enjoy using <strong>NotifyHub</strong>!</p>
                </body>
                </html>
                """;

        notify.to("gabriel@test.com")
                .via(Channel.EMAIL)
                .subject("Welcome to NotifyHub")
                .content(htmlContent)
                .send();

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertEquals(1, received.length);

        MimeMessage email = received[0];
        assertEquals("Welcome to NotifyHub", email.getSubject());
        assertTrue(email.getContentType().contains("text/html"),
                "Should be HTML content type. Got: " + email.getContentType());

        String body = email.getContent().toString();
        assertTrue(body.contains("Welcome, Gabriel!"));
        assertTrue(body.contains("NotifyHub"));

        System.out.println("=== HTML EMAIL RECEIVED ===");
        System.out.println("Content-Type: " + email.getContentType());
        System.out.println("Body: " + body);
        System.out.println("===========================");
    }

    @Test
    @DisplayName("Should send email with Mustache template rendered")
    void sendRealEmailWithTemplate() throws Exception {
        notify.to("customer@test.com")
                .via(Channel.EMAIL)
                .subject("Order Confirmed!")
                .template("order-confirmed")
                .param("customerName", "Gabriel")
                .param("orderId", "ORD-12345")
                .param("total", "R$ 299,90")
                .send();

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertEquals(1, received.length);

        MimeMessage email = received[0];
        assertEquals("Order Confirmed!", email.getSubject());

        String body = email.getContent().toString();
        assertTrue(body.contains("Gabriel"), "Should contain customer name");
        assertTrue(body.contains("ORD-12345"), "Should contain order ID");
        assertTrue(body.contains("R$ 299,90"), "Should contain total");

        System.out.println("=== TEMPLATE EMAIL RECEIVED ===");
        System.out.println("Subject: " + email.getSubject());
        System.out.println("Body: " + body);
        System.out.println("================================");
    }

    @Test
    @DisplayName("Should send email using Notifiable interface")
    void sendRealEmailToNotifiable() throws Exception {
        Notifiable user = new Notifiable() {
            public String getNotifyEmail() { return "user@company.com"; }
            public String getNotifyName() { return "Maria Silva"; }
        };

        notify.to(user)
                .via(Channel.EMAIL)
                .subject("Your report is ready")
                .content("Hi! Your monthly report has been generated.")
                .send();

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertEquals(1, received.length);
        assertEquals("user@company.com", received[0].getAllRecipients()[0].toString());
        assertEquals("Your report is ready", received[0].getSubject());

        System.out.println("=== NOTIFIABLE EMAIL ===");
        System.out.println("To: " + received[0].getAllRecipients()[0]);
        System.out.println("Subject: " + received[0].getSubject());
        System.out.println("========================");
    }

    @Test
    @DisplayName("Should send multiple emails sequentially")
    void sendMultipleEmails() throws Exception {
        notify.to("user1@test.com")
                .via(Channel.EMAIL)
                .subject("Email 1")
                .content("First email")
                .send();

        notify.to("user2@test.com")
                .via(Channel.EMAIL)
                .subject("Email 2")
                .content("Second email")
                .send();

        notify.to("user3@test.com")
                .via(Channel.EMAIL)
                .subject("Email 3")
                .content("Third email")
                .send();

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertEquals(3, received.length, "Should have received 3 emails");

        System.out.println("=== MULTIPLE EMAILS ===");
        for (int i = 0; i < received.length; i++) {
            System.out.println((i + 1) + ". To: " + received[i].getAllRecipients()[0]
                    + " | Subject: " + received[i].getSubject());
        }
        System.out.println("=======================");
    }

    @Test
    @DisplayName("Should fail with wrong SMTP config and throw exception")
    void failWithBadConfig() {
        SmtpConfig badConfig = SmtpConfig.builder()
                .host("localhost")
                .port(9999)  // wrong port — nothing listening here
                .from("noreply@test.com")
                .tls(false)
                .build();

        NotifyHub badHub = NotifyHub.builder()
                .channel(new SmtpEmailChannel(badConfig))
                .build();

        assertThrows(NotificationSendException.class, () ->
                badHub.to("user@test.com")
                        .via(Channel.EMAIL)
                        .content("This should fail")
                        .send()
        );

        System.out.println("=== BAD CONFIG TEST PASSED — exception thrown as expected ===");
    }
}
