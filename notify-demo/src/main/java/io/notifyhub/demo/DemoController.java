package io.notifyhub.demo;

import io.notifyhub.core.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * General demo endpoints: API info, tracking history, inbox, and clear.
 */
@RestController
@Tag(name = "General", description = "API info, inbox, tracking, and maintenance endpoints")
public class DemoController {

    private final NotifyHub notify;
    private final DemoSlackChannel slackChannel;
    private final DemoSchedulingController schedulingController;

    @Autowired(required = false)
    private EmbeddedSmtpConfig smtpConfig;

    @Autowired(required = false)
    private NotificationTracker tracker;

    public DemoController(NotifyHub notify, DemoSlackChannel slackChannel,
                          DemoSchedulingController schedulingController) {
        this.notify = notify;
        this.slackChannel = slackChannel;
        this.schedulingController = schedulingController;
    }

    // ===================== API INFO =====================

    @Operation(summary = "API Info", description = "Lists all available endpoints and registered channels")
    @GetMapping("/api/info")
    public Map<String, Object> home() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("app", "NotifyHub Demo");
        response.put("channels", notify.getRegisteredChannels());
        response.put("profile", smtpConfig != null ? "default (embedded SMTP)" : "real (external SMTP)");
        response.put("tracking", tracker != null ? "enabled" : "disabled");
        response.put("endpoints", List.of(
                "GET  /                          → home page (HTML)",
                "GET  /api/info                  → this endpoint (JSON)",
                "POST /send/email                → send a simple email",
                "POST /send/template             → send email with Mustache template",
                "POST /send/notifiable           → send to a Notifiable user entity",
                "POST /send/sms                  → send SMS via Twilio",
                "POST /send/whatsapp             → send WhatsApp via Twilio",
                "POST /send/telegram             → send to Telegram via Bot",
                "POST /send/discord              → send to Discord via Webhook",
                "POST /send/slack                → send to Slack channel",
                "POST /send/teams                → send to Microsoft Teams via Webhook",
                "POST /send/google-chat          → send to Google Chat via Webhook",
                "POST /send/push                 → send push notification via Firebase",
                "POST /send/websocket            → send message via WebSocket",
                "POST /send/twitter              → post a tweet on Twitter/X",
                "POST /send/linkedin             → publish a post on LinkedIn",
                "POST /send/notion               → create a page in Notion",
                "POST /send/multi                → send to email + slack simultaneously",
                "POST /send/fallback             → test fallback (email fails → slack)",
                "POST /send/tracked              → send with delivery tracking",
                "POST /send/scheduled            → schedule notification for future delivery",
                "GET  /scheduled                 → list all scheduled notifications",
                "DELETE /scheduled/{id}          → cancel a scheduled notification",
                "GET  /tracking                  → delivery tracking history",
                "GET  /inbox                     → see all received emails (embedded only)",
                "GET  /inbox/slack               → see all Slack messages",
                "DELETE /inbox                   → clear all inboxes",
                "POST /contacts                  → create a contact with tags",
                "GET  /contacts                  → list all contacts",
                "POST /contacts/seed             → seed sample contacts for testing",
                "POST /audiences                 → create a named audience",
                "POST /send/audience             → send to all contacts in an audience"
        ));
        return response;
    }

    // ===================== TRACKING =====================

    @Operation(summary = "Delivery history", description = "View delivery tracking receipts")
    @GetMapping("/tracking")
    public Map<String, Object> trackingHistory() {
        if (tracker == null) {
            return Map.of(
                    "info", "Delivery tracking is not enabled",
                    "tip", "Set notify.tracking.enabled=true in application.properties"
            );
        }

        List<Map<String, Object>> receipts = new ArrayList<>();
        for (DeliveryReceipt r : tracker.findAll()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId());
            item.put("channel", r.getChannelName());
            item.put("recipient", r.getRecipient());
            item.put("status", r.getStatus().name());
            item.put("timestamp", r.getTimestamp().toString());
            if (r.getErrorMessage() != null) {
                item.put("error", r.getErrorMessage());
            }
            receipts.add(item);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", tracker.count());
        response.put("receipts", receipts);
        return response;
    }

    // ===================== INBOX =====================

    @Operation(summary = "Email inbox", description = "View all received emails (embedded SMTP only)")
    @GetMapping("/inbox")
    public Map<String, Object> inbox() throws Exception {
        if (smtpConfig == null) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("info", "Inbox not available — using real SMTP, emails go to your real inbox");
            return response;
        }

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

    // ===================== CLEAR INBOX =====================

    @Operation(summary = "Clear all inboxes", description = "Clear email inbox, Slack inbox, tracking data, and scheduled notifications")
    @DeleteMapping("/inbox")
    public Map<String, String> clearInbox() {
        if (smtpConfig != null) {
            try {
                smtpConfig.getGreenMail().purgeEmailFromAllMailboxes();
            } catch (Exception ignored) {
            }
        }
        slackChannel.clear();
        if (tracker != null) {
            tracker.clear();
        }
        schedulingController.getScheduledMap().clear();
        return Map.of("status", "All inboxes and tracking data cleared");
    }
}
