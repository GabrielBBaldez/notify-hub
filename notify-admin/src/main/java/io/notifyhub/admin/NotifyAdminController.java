package io.notifyhub.admin;

import io.notifyhub.core.DeliveryReceipt;
import io.notifyhub.core.DeliveryStatus;
import io.notifyhub.core.NotificationTracker;
import io.notifyhub.core.NotifyHub;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.dlq.DeadLetter;
import io.notifyhub.core.dlq.DeadLetterQueue;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Controller
@RequestMapping("/notify-admin")
public class NotifyAdminController {

    private final NotifyHub hub;

    public NotifyAdminController(NotifyHub hub) {
        this.hub = hub;
    }

    /** Dashboard -- overview with totals. */
    @GetMapping
    public String dashboard(Model model) {
        Set<String> channels = hub.getRegisteredChannels();
        model.addAttribute("channelCount", channels.size());
        model.addAttribute("channels", channels);

        NotificationTracker tracker = hub.getTracker();
        long totalSent = 0;
        long totalFailed = 0;
        long totalPending = 0;
        if (tracker != null) {
            List<DeliveryReceipt> all = tracker.findAll();
            totalSent = all.stream()
                    .filter(r -> r.getStatus() == DeliveryStatus.SENT)
                    .count();
            totalFailed = all.stream()
                    .filter(r -> r.getStatus() == DeliveryStatus.FAILED)
                    .count();
            totalPending = all.stream()
                    .filter(r -> r.getStatus() == DeliveryStatus.PENDING
                            || r.getStatus() == DeliveryStatus.SCHEDULED)
                    .count();
        }
        model.addAttribute("totalSent", totalSent);
        model.addAttribute("totalFailed", totalFailed);
        model.addAttribute("totalPending", totalPending);
        model.addAttribute("trackingEnabled", tracker != null);

        DeadLetterQueue dlq = hub.getDeadLetterQueue();
        model.addAttribute("dlqCount", dlq != null ? dlq.count() : 0);
        model.addAttribute("dlqEnabled", dlq != null);

        return "notify-admin/dashboard";
    }

    /** Tracking -- list all delivery receipts with optional channel filter. */
    @GetMapping("/tracking")
    public String tracking(@RequestParam(name = "channel", required = false) String channel, Model model) {
        NotificationTracker tracker = hub.getTracker();
        List<DeliveryReceipt> receipts = Collections.emptyList();
        if (tracker != null) {
            if (channel != null && !channel.isBlank()) {
                receipts = tracker.findAll().stream()
                        .filter(r -> channel.equalsIgnoreCase(r.getChannelName()))
                        .toList();
            } else {
                receipts = tracker.findAll();
            }
        }
        model.addAttribute("receipts", receipts);
        model.addAttribute("channels", hub.getRegisteredChannels());
        model.addAttribute("selectedChannel", channel);
        model.addAttribute("trackingEnabled", tracker != null);
        return "notify-admin/tracking";
    }

    /** DLQ -- list dead letters. */
    @GetMapping("/dlq")
    public String dlq(Model model) {
        DeadLetterQueue dlq = hub.getDeadLetterQueue();
        List<DeadLetter> deadLetters = dlq != null ? dlq.findAll() : Collections.emptyList();
        model.addAttribute("deadLetters", deadLetters);
        model.addAttribute("dlqEnabled", dlq != null);
        model.addAttribute("dlqCount", dlq != null ? dlq.count() : 0);
        return "notify-admin/dlq";
    }

    /** Remove a dead letter from the DLQ. */
    @PostMapping("/dlq/{id}/reprocess")
    public String reprocessDeadLetter(@PathVariable("id") String id) {
        DeadLetterQueue dlq = hub.getDeadLetterQueue();
        if (dlq != null) {
            dlq.remove(id);
        }
        return "redirect:/notify-admin/dlq";
    }

    /** Channels -- status of each registered channel. */
    @GetMapping("/channels")
    public String channels(Model model) {
        Set<String> channelNames = hub.getRegisteredChannels();
        List<Map<String, Object>> channelInfos = new ArrayList<>();
        for (String name : channelNames) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("name", name);
            Optional<NotificationChannel> ch = hub.getChannel(name);
            info.put("available", ch.map(NotificationChannel::isAvailable).orElse(false));
            info.put("type", ch.map(c -> c.getClass().getSimpleName()).orElse("Unknown"));
            channelInfos.add(info);
        }
        model.addAttribute("channelInfos", channelInfos);
        return "notify-admin/channels";
    }
}
