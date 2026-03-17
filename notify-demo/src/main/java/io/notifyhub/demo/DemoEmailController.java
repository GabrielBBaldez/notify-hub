package io.notifyhub.demo;

import io.notifyhub.core.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Email-related demo endpoints: simple email, template email, notifiable, tracked, fallback.
 */
@RestController
@Tag(name = "Email", description = "Email notifications via SMTP")
public class DemoEmailController {

    private final NotifyHub notify;
    private final DemoSlackChannel slackChannel;

    @Autowired(required = false)
    private EmbeddedSmtpConfig smtpConfig;

    @Autowired(required = false)
    private NotificationTracker tracker;

    public DemoEmailController(NotifyHub notify, DemoSlackChannel slackChannel) {
        this.notify = notify;
        this.slackChannel = slackChannel;
    }

    // ===================== SIMPLE EMAIL =====================

    @Operation(summary = "Send email", description = "Send a simple email via SMTP")
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
                "tip", smtpConfig != null
                        ? "Check GET /inbox to see the received email"
                        : "Check your real inbox at " + to
        );
    }

    // ===================== TEMPLATE EMAIL =====================

    @Operation(summary = "Send email with template", description = "Send an email rendered with a Mustache template")
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
                "tip", smtpConfig != null
                        ? "Check GET /inbox to see the rendered HTML email"
                        : "Check your real inbox for a beautiful HTML email!"
        );
    }

    // ===================== NOTIFIABLE USER =====================

    @Operation(summary = "Send to Notifiable entity", description = "Send email to a user implementing the Notifiable interface")
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

    // ===================== TRACKED SEND =====================

    @Operation(summary = "Send with tracking", description = "Send email and get a delivery receipt")
    @PostMapping("/send/tracked")
    public Map<String, Object> sendTracked(
            @RequestParam(defaultValue = "tracked@test.com") String to,
            @RequestParam(defaultValue = "Tracked Notification") String subject,
            @RequestParam(defaultValue = "This email has delivery tracking enabled!") String body) {

        DeliveryReceipt receipt = notify.to(to)
                .via(Channel.EMAIL)
                .subject(subject)
                .content(body)
                .sendTracked();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "sent with tracking");
        response.put("receipt", Map.of(
                "id", receipt.getId(),
                "channel", receipt.getChannelName(),
                "recipient", receipt.getRecipient(),
                "deliveryStatus", receipt.getStatus().name(),
                "timestamp", receipt.getTimestamp().toString()
        ));
        response.put("tip", "Check GET /tracking to see delivery history");
        return response;
    }

    // ===================== FALLBACK =====================

    @Operation(summary = "Test fallback", description = "Test fallback chain: email fails, falls back to Slack")
    @PostMapping("/send/fallback")
    public Map<String, String> sendFallback() {
        if (smtpConfig == null) {
            return Map.of(
                    "error", "Fallback demo only works with embedded SMTP (default profile)",
                    "tip", "Run without -Dspring-boot.run.profiles=real to test fallback"
            );
        }

        // Stop SMTP to force email failure
        smtpConfig.getGreenMail().stop();

        try {
            notify.to("fallback-test@test.com")
                    .via(Channel.EMAIL)
                    .fallback(Channel.SLACK)
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
}
