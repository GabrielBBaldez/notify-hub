package io.notifyhub.demo;

import io.notifyhub.core.Channel;
import io.notifyhub.core.NotifyHub;
import io.notifyhub.core.channel.NotificationSendException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Demo endpoints showcasing every NotifyHub feature.
 * Run the app and try each endpoint!
 */
@RestController
public class DemoController {

    private final NotifyHub notify;
    private final EmbeddedSmtpConfig smtpConfig;
    private final SlackChannel slackChannel;

    public DemoController(NotifyHub notify, EmbeddedSmtpConfig smtpConfig, SlackChannel slackChannel) {
        this.notify = notify;
        this.smtpConfig = smtpConfig;
        this.slackChannel = slackChannel;
    }

    // ===================== HOME =====================

    @GetMapping("/")
    public Map<String, Object> home() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("app", "NotifyHub Demo");
        response.put("channels", notify.getRegisteredChannels());
        response.put("endpoints", List.of(
                "GET  /                          → this page",
                "POST /send/email                → send a simple email",
                "POST /send/template             → send email with Mustache template",
                "POST /send/notifiable           → send to a Notifiable user entity",
                "POST /send/slack                → send to custom Slack channel",
                "POST /send/multi                → send to email + slack simultaneously",
                "POST /send/fallback             → test fallback (email fails → slack)",
                "GET  /inbox                     → see all received emails",
                "GET  /inbox/slack               → see all Slack messages",
                "DELETE /inbox                   → clear all inboxes"
        ));
        return response;
    }

    // ===================== 1. SIMPLE EMAIL =====================

    @PostMapping("/send/email")
    public Map<String, String> sendEmail(
            @RequestParam(defaultValue = "demo@test.com") String to,
            @RequestParam(defaultValue = "Hello from NotifyHub!") String subject,
            @RequestParam(defaultValue = "This email was sent by the NotifyHub demo app.") String body) {

        notify.to(to)
                .via(Channel.EMAIL)
                .subject(subject)
                .content(body)
                .send();

        return Map.of(
                "status", "sent",
                "channel", "email",
                "to", to,
                "tip", "Check GET /inbox to see the received email"
        );
    }

    // ===================== 2. TEMPLATE EMAIL =====================

    @PostMapping("/send/template")
    public Map<String, String> sendTemplate(
            @RequestParam(defaultValue = "customer@test.com") String to,
            @RequestParam(defaultValue = "Gabriel") String customerName,
            @RequestParam(defaultValue = "ORD-99887") String orderId,
            @RequestParam(defaultValue = "R$ 459,90") String total) {

        notify.to(to)
                .via(Channel.EMAIL)
                .subject("Order Confirmed - #" + orderId)
                .template("order-confirmed")
                .param("customerName", customerName)
                .param("orderId", orderId)
                .param("total", total)
                .send();

        return Map.of(
                "status", "sent",
                "channel", "email",
                "template", "order-confirmed",
                "to", to,
                "tip", "Check GET /inbox to see the rendered HTML email"
        );
    }

    // ===================== 3. NOTIFIABLE USER =====================

    @PostMapping("/send/notifiable")
    public Map<String, String> sendToNotifiable(
            @RequestParam(defaultValue = "Maria Silva") String name,
            @RequestParam(defaultValue = "maria@company.com") String email,
            @RequestParam(defaultValue = "Pro") String plan) {

        FakeUser user = new FakeUser(name, email, "+5548999999999");

        notify.to(user)
                .via(Channel.EMAIL)
                .subject("Welcome, " + name + "!")
                .template("welcome")
                .param("name", name)
                .param("plan", plan)
                .send();

        return Map.of(
                "status", "sent",
                "channel", "email",
                "notifiable", name + " <" + email + ">",
                "tip", "NotifyHub resolved the email from the Notifiable interface"
        );
    }

    // ===================== 4. CUSTOM CHANNEL (SLACK) =====================

    @PostMapping("/send/slack")
    public Map<String, String> sendSlack(
            @RequestParam(defaultValue = "#general") String channel,
            @RequestParam(defaultValue = "Deploy v2.1.0 completed successfully!") String message) {

        notify.to(channel)
                .via(Channel.custom("slack"))
                .content(message)
                .send();

        return Map.of(
                "status", "sent",
                "channel", "slack",
                "to", channel,
                "tip", "Check GET /inbox/slack to see the message"
        );
    }

    // ===================== 5. MULTI-CHANNEL =====================

    @PostMapping("/send/multi")
    public Map<String, String> sendMulti(
            @RequestParam(defaultValue = "security@test.com") String email,
            @RequestParam(defaultValue = "#security-alerts") String slackChannel) {

        FakeUser user = new FakeUser("Admin", email, null);

        notify.to(user)
                .via(Channel.EMAIL)
                .via(Channel.custom("slack"))
                .subject("Security Alert")
                .content("Login detected from a new device: Windows 11, Chrome 120, IP: 189.40.xx.xx")
                .sendAll();

        return Map.of(
                "status", "sent to ALL channels",
                "channels", "email + slack",
                "email_to", email,
                "slack_to", slackChannel,
                "tip", "Check GET /inbox AND GET /inbox/slack — both received!"
        );
    }

    // ===================== 6. FALLBACK =====================

    @PostMapping("/send/fallback")
    public Map<String, String> sendFallback() {
        // Stop SMTP to force email failure
        smtpConfig.getGreenMail().stop();

        try {
            notify.to("fallback-test@test.com")
                    .via(Channel.EMAIL)
                    .fallback(Channel.custom("slack"))
                    .subject("Important notification")
                    .content("This message was supposed to go via email, but email failed so it went to Slack!")
                    .send();

            return Map.of(
                    "status", "fallback triggered",
                    "primary", "email (FAILED — SMTP was stopped)",
                    "fallback", "slack (SUCCESS)",
                    "tip", "Check GET /inbox/slack — the message landed there!"
            );
        } finally {
            smtpConfig.getGreenMail().start();
        }
    }

    // ===================== INBOX =====================

    @GetMapping("/inbox")
    public Map<String, Object> inbox() throws Exception {
        MimeMessage[] messages = smtpConfig.getGreenMail().getReceivedMessages();
        List<Map<String, String>> emails = new ArrayList<>();

        for (MimeMessage msg : messages) {
            Map<String, String> email = new LinkedHashMap<>();
            email.put("from", msg.getFrom()[0].toString());
            email.put("to", msg.getAllRecipients()[0].toString());
            email.put("subject", msg.getSubject());
            email.put("body", msg.getContent().toString().trim());
            email.put("contentType", msg.getContentType());
            emails.add(email);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", emails.size());
        response.put("emails", emails);
        return response;
    }

    @GetMapping("/inbox/slack")
    public Map<String, Object> slackInbox() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", slackChannel.getMessages().size());
        response.put("messages", slackChannel.getMessages());
        return response;
    }

    @DeleteMapping("/inbox")
    public Map<String, String> clearInbox() {
        try {
            smtpConfig.getGreenMail().purgeEmailFromAllMailboxes();
        } catch (Exception ignored) {
        }
        slackChannel.clear();
        return Map.of("status", "All inboxes cleared");
    }
}
