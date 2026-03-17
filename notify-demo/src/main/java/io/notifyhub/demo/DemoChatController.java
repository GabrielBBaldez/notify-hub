package io.notifyhub.demo;

import io.notifyhub.core.Channel;
import io.notifyhub.core.NotifyHub;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Chat platform demo endpoints: Slack, Discord, Teams, Telegram, Google Chat.
 */
@RestController
@Tag(name = "Chat Platforms", description = "Notifications via Slack, Discord, Teams, Telegram, Google Chat")
public class DemoChatController {

    private final NotifyHub notify;
    private final DemoSlackChannel slackChannel;

    public DemoChatController(NotifyHub notify, DemoSlackChannel slackChannel) {
        this.notify = notify;
        this.slackChannel = slackChannel;
    }

    // ===================== SLACK =====================

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

    // ===================== TELEGRAM =====================

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

    // ===================== DISCORD =====================

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

    // ===================== SLACK INBOX =====================

    @Operation(summary = "Slack inbox", description = "View all received Slack messages (demo only)")
    @GetMapping("/inbox/slack")
    public Map<String, Object> slackInbox() {
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("total", slackChannel.getMessages().size());
        response.put("messages", slackChannel.getMessages());
        return response;
    }
}
