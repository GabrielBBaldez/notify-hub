package io.notifyhub.demo;

import io.notifyhub.core.*;
import io.notifyhub.core.channel.NotificationSendException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Demo endpoints showcasing every NotifyHub feature.
 * Run the app and try each endpoint!
 */
@RestController
@Tag(name = "NotifyHub API", description = "Unified notification API — send via Email, SMS, WhatsApp, Slack, Telegram, Discord, Teams, Google Chat, and more")
public class DemoController {

    private final NotifyHub notify;
    private final DemoSlackChannel slackChannel;

    /** Stores scheduled notifications by ID for inspection */
    private final Map<String, ScheduledNotification> scheduledMap = new ConcurrentHashMap<>();

    /** Nullable — only present when running with embedded SMTP (default profile) */
    @Autowired(required = false)
    private EmbeddedSmtpConfig smtpConfig;

    /** Nullable — only present when tracking is enabled */
    @Autowired(required = false)
    private NotificationTracker tracker;

    public DemoController(NotifyHub notify, DemoSlackChannel slackChannel) {
        this.notify = notify;
        this.slackChannel = slackChannel;
    }

    // ===================== HOME =====================

    @Operation(summary = "Home", description = "Lists all available endpoints and registered channels")
    @GetMapping("/")
    public Map<String, Object> home() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("app", "NotifyHub Demo");
        response.put("channels", notify.getRegisteredChannels());
        response.put("profile", smtpConfig != null ? "default (embedded SMTP)" : "real (external SMTP)");
        response.put("tracking", tracker != null ? "enabled" : "disabled");
        response.put("endpoints", List.of(
                "GET  /                          → this page",
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
                "DELETE /inbox                   → clear all inboxes"
        ));
        return response;
    }

    // ===================== 1. SIMPLE EMAIL =====================

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

    // ===================== 2. TEMPLATE EMAIL =====================

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

    // ===================== 3. NOTIFIABLE USER =====================

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

    // ===================== 4. SMS (TWILIO) =====================

    @Operation(summary = "Send SMS", description = "Send SMS via Twilio")
    @PostMapping("/send/sms")
    public Map<String, String> sendSms(
            @RequestParam String to,
            @RequestParam(defaultValue = "Hello from NotifyHub! Your notification system is working.") String message) {

        notify.toPhone(to)
                .via(Channel.SMS)
                .content(message)
                .send();

        return Map.of(
                "status", "sent",
                "channel", "sms",
                "to", to,
                "tip", "Check your phone for the SMS!"
        );
    }

    // ===================== 5. WHATSAPP (TWILIO) =====================

    @Operation(summary = "Send WhatsApp", description = "Send WhatsApp message via Twilio")
    @PostMapping("/send/whatsapp")
    public Map<String, String> sendWhatsApp(
            @RequestParam String to,
            @RequestParam(defaultValue = "Hello from NotifyHub via WhatsApp!") String message) {

        notify.toPhone(to)
                .via(Channel.WHATSAPP)
                .content(message)
                .send();

        return Map.of(
                "status", "sent",
                "channel", "whatsapp",
                "to", to,
                "tip", "Check your WhatsApp!"
        );
    }

    // ===================== 6. TELEGRAM =====================

    @Operation(summary = "Send Telegram", description = "Send message via Telegram Bot API")
    @PostMapping("/send/telegram")
    public Map<String, String> sendTelegram(
            @RequestParam String chatId,
            @RequestParam(defaultValue = "Hello from NotifyHub via Telegram! 🚀") String message) {

        notify.to(chatId)
                .via(Channel.TELEGRAM)
                .content(message)
                .send();

        return Map.of(
                "status", "sent",
                "channel", "telegram",
                "to", chatId,
                "tip", "Check your Telegram chat!"
        );
    }

    // ===================== 7. DISCORD =====================

    @Operation(summary = "Send Discord", description = "Send message to Discord channel via webhook. Use a named recipient alias or default.")
    @PostMapping("/send/discord")
    public Map<String, String> sendDiscord(
            @RequestParam(defaultValue = "discord-channel") String to,
            @RequestParam(defaultValue = "Hello from NotifyHub via Discord! 🎮") String message) {

        notify.to(to)
                .via(Channel.DISCORD)
                .content(message)
                .send();

        return Map.of(
                "status", "sent",
                "channel", "discord",
                "to", to,
                "tip", "Check your Discord channel!"
        );
    }

    // ===================== TEAMS =====================

    @Operation(summary = "Send Teams", description = "Send message to Microsoft Teams channel via webhook")
    @PostMapping("/send/teams")
    public Map<String, String> sendTeams(
            @RequestParam(defaultValue = "default") String to,
            @RequestParam(defaultValue = "Hello from NotifyHub via Teams!") String message) {

        notify.to(to)
                .via(Channel.TEAMS)
                .content(message)
                .send();

        return Map.of(
                "status", "sent",
                "channel", "teams",
                "to", to,
                "tip", "Check your Teams channel!"
        );
    }

    // ===================== GOOGLE CHAT =====================

    @Operation(summary = "Send Google Chat", description = "Send message to Google Chat space via webhook. Use a named recipient alias or default.")
    @PostMapping("/send/google-chat")
    public Map<String, String> sendGoogleChat(
            @RequestParam(defaultValue = "default") String to,
            @RequestParam(defaultValue = "Hello from NotifyHub via Google Chat!") String message) {

        notify.to(to)
                .via(Channel.GOOGLE_CHAT)
                .content(message)
                .send();

        return Map.of(
                "status", "sent",
                "channel", "google-chat",
                "to", to,
                "tip", "Check your Google Chat space!"
        );
    }

    // ===================== PUSH (FIREBASE) =====================

    @Operation(summary = "Send Push Notification", description = "Send push notification via Firebase Cloud Messaging (FCM). Requires a device token.")
    @PostMapping("/send/push")
    public Map<String, String> sendPush(
            @RequestParam String deviceToken,
            @RequestParam(defaultValue = "NotifyHub Alert") String title,
            @RequestParam(defaultValue = "Hello from NotifyHub via Push!") String message) {

        notify.to(deviceToken)
                .via(Channel.PUSH)
                .subject(title)
                .content(message)
                .send();

        return Map.of(
                "status", "sent",
                "channel", "push",
                "to", deviceToken,
                "tip", "Check your device for the push notification!"
        );
    }

    // ===================== WEBSOCKET =====================

    @Operation(summary = "Send WebSocket", description = "Send message via WebSocket connection. Connects, sends, and closes.")
    @PostMapping("/send/websocket")
    public Map<String, String> sendWebSocket(
            @RequestParam(defaultValue = "default") String to,
            @RequestParam(defaultValue = "Hello from NotifyHub via WebSocket!") String message) {

        notify.to(to)
                .via(Channel.WEBSOCKET)
                .content(message)
                .send();

        return Map.of(
                "status", "sent",
                "channel", "websocket",
                "to", to,
                "tip", "Message sent via WebSocket!"
        );
    }

    // ===================== TWITTER/X =====================

    @Operation(summary = "Post Tweet", description = "Post a tweet on Twitter/X via API v2")
    @PostMapping("/send/twitter")
    public Map<String, String> sendTwitter(
            @RequestParam(defaultValue = "Hello from NotifyHub via Twitter/X!") String message) {

        notify.to("default")
                .via(Channel.TWITTER)
                .content(message)
                .send();

        return Map.of(
                "status", "sent",
                "channel", "twitter",
                "tip", "Check your Twitter/X profile for the tweet!"
        );
    }

    // ===================== LINKEDIN =====================

    @Operation(summary = "Publish LinkedIn Post", description = "Publish a post on LinkedIn via REST API")
    @PostMapping("/send/linkedin")
    public Map<String, String> sendLinkedIn(
            @RequestParam(defaultValue = "default") String to,
            @RequestParam(defaultValue = "Hello from NotifyHub via LinkedIn!") String message) {

        notify.to(to)
                .via(Channel.LINKEDIN)
                .content(message)
                .send();

        return Map.of(
                "status", "sent",
                "channel", "linkedin",
                "to", to,
                "tip", "Check your LinkedIn feed for the post!"
        );
    }

    // ===================== NOTION =====================

    @Operation(summary = "Create Notion Page", description = "Create a page in a Notion database via API")
    @PostMapping("/send/notion")
    public Map<String, String> sendNotion(
            @RequestParam(defaultValue = "default") String to,
            @RequestParam(defaultValue = "Notification") String title,
            @RequestParam(defaultValue = "Hello from NotifyHub via Notion!") String message) {

        notify.to(to)
                .via(Channel.NOTION)
                .subject(title)
                .content(message)
                .send();

        return Map.of(
                "status", "sent",
                "channel", "notion",
                "to", to,
                "tip", "Check your Notion database for the new page!"
        );
    }

    // ===================== 8. SLACK =====================

    @Operation(summary = "Send Slack", description = "Send message to Slack channel via webhook")
    @PostMapping("/send/slack")
    public Map<String, String> sendSlack(
            @RequestParam(defaultValue = "#general") String channel,
            @RequestParam(defaultValue = "Deploy v2.1.0 completed successfully!") String message) {

        notify.to(channel)
                .via(Channel.SLACK)
                .content(message)
                .send();

        return Map.of(
                "status", "sent",
                "channel", "slack",
                "to", channel,
                "tip", "Check GET /inbox/slack to see the message"
        );
    }

    // ===================== 7. MULTI-CHANNEL =====================

    @Operation(summary = "Send multi-channel", description = "Send to Email + Slack simultaneously")
    @PostMapping("/send/multi")
    public Map<String, String> sendMulti(
            @RequestParam(defaultValue = "security@test.com") String email,
            @RequestParam(defaultValue = "#security-alerts") String slackChannel) {

        FakeUser user = new FakeUser("Admin", email, null);

        notify.to(user)
                .via(Channel.EMAIL)
                .via(Channel.SLACK)
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

    // ===================== 8. FALLBACK =====================

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

    // ===================== 9. TRACKED SEND =====================

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

    // ===================== 10. SCHEDULED NOTIFICATION =====================

    @Operation(summary = "Schedule notification", description = "Schedule a notification for future delivery")
    @PostMapping("/send/scheduled")
    public Map<String, Object> sendScheduled(
            @RequestParam(defaultValue = "scheduled@test.com") String to,
            @RequestParam(defaultValue = "30") int delaySeconds,
            @RequestParam(defaultValue = "Scheduled Reminder") String subject,
            @RequestParam(defaultValue = "This notification was scheduled and sent automatically!") String body) {

        ScheduledNotification scheduled = notify.to(to)
                .via(Channel.EMAIL)
                .subject(subject)
                .content(body)
                .schedule(Duration.ofSeconds(delaySeconds));

        scheduledMap.put(scheduled.getId(), scheduled);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "scheduled");
        response.put("id", scheduled.getId());
        response.put("channel", scheduled.getChannelName());
        response.put("recipient", scheduled.getRecipient());
        response.put("scheduledTime", scheduled.getScheduledTime().toString());
        response.put("remainingDelay", scheduled.getRemainingDelay().getSeconds() + "s");
        response.put("tip", "The notification will fire in " + delaySeconds + " seconds. " +
                "Check GET /scheduled to see status, or DELETE /scheduled/" + scheduled.getId() + " to cancel.");
        return response;
    }

    // ===================== SCHEDULED STATUS =====================

    @GetMapping("/scheduled")
    public Map<String, Object> listScheduled() {
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map.Entry<String, ScheduledNotification> entry : scheduledMap.entrySet()) {
            ScheduledNotification sn = entry.getValue();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", sn.getId());
            item.put("channel", sn.getChannelName());
            item.put("recipient", sn.getRecipient());
            item.put("status", sn.getStatus().name());
            item.put("scheduledTime", sn.getScheduledTime().toString());
            item.put("isDone", sn.isDone());
            item.put("isCancelled", sn.isCancelled());
            item.put("remainingDelay", sn.getRemainingDelay().getSeconds() + "s");
            items.add(item);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", items.size());
        response.put("scheduled", items);
        return response;
    }

    @DeleteMapping("/scheduled/{id}")
    public Map<String, String> cancelScheduled(@PathVariable String id) {
        ScheduledNotification sn = scheduledMap.get(id);
        if (sn == null) {
            return Map.of("error", "Scheduled notification not found: " + id);
        }

        boolean cancelled = sn.cancel();
        return Map.of(
                "id", id,
                "cancelled", String.valueOf(cancelled),
                "status", sn.getStatus().name(),
                "tip", cancelled
                        ? "Notification was successfully cancelled before delivery"
                        : "Could not cancel — it may have already been sent"
        );
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

    @GetMapping("/inbox/slack")
    public Map<String, Object> slackInbox() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", slackChannel.getMessages().size());
        response.put("messages", slackChannel.getMessages());
        return response;
    }

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
        scheduledMap.clear();
        return Map.of("status", "All inboxes and tracking data cleared");
    }
}
