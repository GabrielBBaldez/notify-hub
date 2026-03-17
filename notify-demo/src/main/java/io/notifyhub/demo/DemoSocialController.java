package io.notifyhub.demo;

import io.notifyhub.core.Channel;
import io.notifyhub.core.NotifyHub;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Social media and content platform demo endpoints:
 * Twitter, LinkedIn, Facebook, Instagram, YouTube, Twitch, TikTok Shop, Notion.
 */
@RestController
@Tag(name = "Social & Content", description = "Notifications via Twitter, LinkedIn, Facebook, Instagram, YouTube, Twitch, TikTok Shop, Notion")
public class DemoSocialController {

    private final NotifyHub notify;

    public DemoSocialController(NotifyHub notify) {
        this.notify = notify;
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

    // ===================== FACEBOOK =====================

    @Operation(summary = "Send Facebook", description = "Send message via Facebook Graph API")
    @PostMapping("/send/facebook")
    public Map<String, String> sendFacebook(
            @RequestParam(defaultValue = "default") String to,
            @RequestParam(defaultValue = "Hello from NotifyHub via Facebook!") String message) {

        notify.to(to)
                .via(Channel.FACEBOOK)
                .content(message)
                .send();

        return Map.of(
                "status", "sent",
                "channel", "facebook",
                "to", to,
                "tip", "Check your Facebook page!"
        );
    }

    // ===================== INSTAGRAM =====================

    @Operation(summary = "Send Instagram", description = "Send message via Instagram Graph API")
    @PostMapping("/send/instagram")
    public Map<String, String> sendInstagram(
            @RequestParam(defaultValue = "default") String to,
            @RequestParam(defaultValue = "Hello from NotifyHub via Instagram!") String message) {

        notify.to(to)
                .via(Channel.INSTAGRAM)
                .content(message)
                .send();

        return Map.of(
                "status", "sent",
                "channel", "instagram",
                "to", to,
                "tip", "Check your Instagram!"
        );
    }

    // ===================== YOUTUBE =====================

    @Operation(summary = "Send YouTube", description = "Send message via YouTube Live Chat API")
    @PostMapping("/send/youtube")
    public Map<String, String> sendYouTube(
            @RequestParam(defaultValue = "default") String to,
            @RequestParam(defaultValue = "Hello from NotifyHub via YouTube!") String message) {

        notify.to(to)
                .via(Channel.YOUTUBE)
                .content(message)
                .send();

        return Map.of(
                "status", "sent",
                "channel", "youtube",
                "to", to,
                "tip", "Check your YouTube live chat!"
        );
    }

    // ===================== TWITCH =====================

    @Operation(summary = "Send Twitch", description = "Send message via Twitch Helix API")
    @PostMapping("/send/twitch")
    public Map<String, String> sendTwitch(
            @RequestParam(defaultValue = "default") String to,
            @RequestParam(defaultValue = "Hello from NotifyHub via Twitch!") String message) {

        notify.to(to)
                .via(Channel.TWITCH)
                .content(message)
                .send();

        return Map.of(
                "status", "sent",
                "channel", "twitch",
                "to", to,
                "tip", "Check your Twitch chat!"
        );
    }

    // ===================== TIKTOK SHOP =====================

    @Operation(summary = "Send TikTok Shop", description = "Send notification via TikTok Shop API")
    @PostMapping("/send/tiktok-shop")
    public Map<String, String> sendTikTokShop(
            @RequestParam(defaultValue = "default") String to,
            @RequestParam(defaultValue = "Hello from NotifyHub via TikTok Shop!") String message) {

        notify.to(to)
                .via(Channel.TIKTOK_SHOP)
                .content(message)
                .send();

        return Map.of(
                "status", "sent",
                "channel", "tiktok-shop",
                "to", to,
                "tip", "Check your TikTok Shop notifications!"
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
}
