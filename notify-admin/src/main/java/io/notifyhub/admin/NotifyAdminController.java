package io.notifyhub.admin;

import io.notifyhub.core.AuditEntry;
import io.notifyhub.core.AuditEventType;
import io.notifyhub.core.AuditLog;
import io.notifyhub.core.DeliveryReceipt;
import io.notifyhub.core.DeliveryStatus;
import io.notifyhub.core.NotificationTracker;
import io.notifyhub.core.NotifyHub;
import io.notifyhub.core.StatusWebhookListener;
import io.notifyhub.core.audience.Audience;
import io.notifyhub.core.audience.AudienceManager;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.dlq.DeadLetter;
import io.notifyhub.core.dlq.DeadLetterQueue;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/notify-admin")
public class NotifyAdminController {

    private final NotifyHub hub;
    private final StatusWebhookListener statusWebhookListener;
    private final AudienceManager audienceManager;

    public NotifyAdminController(NotifyHub hub,
                                  ObjectProvider<StatusWebhookListener> webhookListenerProvider,
                                  ObjectProvider<AudienceManager> audienceManagerProvider) {
        this.hub = hub;
        this.statusWebhookListener = webhookListenerProvider.getIfAvailable();
        this.audienceManager = audienceManagerProvider.getIfAvailable();
    }

    /** Dashboard -- overview with totals, recent activity, system status. */
    @GetMapping
    public String dashboard(Model model) {
        Set<String> channelNames = hub.getRegisteredChannels();
        model.addAttribute("channelCount", channelNames.size());

        // Channel infos with availability status
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

        NotificationTracker tracker = hub.getTracker();
        long totalSent = 0;
        long totalFailed = 0;
        long totalPending = 0;
        List<DeliveryReceipt> recentReceipts = Collections.emptyList();
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
            recentReceipts = all.stream()
                    .sorted((a, b) -> {
                        if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
                        if (a.getTimestamp() == null) return 1;
                        if (b.getTimestamp() == null) return -1;
                        return b.getTimestamp().compareTo(a.getTimestamp());
                    })
                    .limit(10)
                    .toList();
        }
        model.addAttribute("totalSent", totalSent);
        model.addAttribute("totalFailed", totalFailed);
        model.addAttribute("totalPending", totalPending);
        model.addAttribute("trackingEnabled", tracker != null);
        model.addAttribute("recentReceipts", recentReceipts);

        DeadLetterQueue dlq = hub.getDeadLetterQueue();
        model.addAttribute("dlqCount", dlq != null ? dlq.count() : 0);
        model.addAttribute("dlqEnabled", dlq != null);

        AuditLog auditLog = hub.getAuditLog();
        model.addAttribute("auditEnabled", auditLog != null);
        model.addAttribute("auditCount", auditLog != null ? auditLog.findAll().size() : 0);

        model.addAttribute("audienceEnabled", audienceManager != null);
        model.addAttribute("contactCount", audienceManager != null
                ? audienceManager.getContactRepository().count() : 0);
        model.addAttribute("audienceCount", audienceManager != null
                ? audienceManager.listAudiences().size() : 0);

        model.addAttribute("webhookConfigured", statusWebhookListener != null);

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

    /** Analytics -- charts page. */
    @GetMapping("/analytics")
    public String analytics() {
        return "notify-admin/analytics";
    }

    /** Audit Log -- list all audit entries with optional event type filter. */
    @GetMapping("/audit-log")
    public String auditLog(@RequestParam(name = "event", required = false) String event, Model model) {
        AuditLog auditLog = hub.getAuditLog();
        List<AuditEntry> entries = Collections.emptyList();
        if (auditLog != null) {
            if (event != null && !event.isBlank()) {
                try {
                    entries = auditLog.findByEventType(AuditEventType.valueOf(event));
                } catch (IllegalArgumentException e) {
                    entries = auditLog.findAll();
                }
            } else {
                entries = auditLog.findAll();
            }
        }
        model.addAttribute("entries", entries);
        model.addAttribute("eventTypes", AuditEventType.values());
        model.addAttribute("selectedEvent", event);
        model.addAttribute("auditEnabled", auditLog != null);
        return "notify-admin/audit-log";
    }

    /** Audiences -- list contacts and audience segments. */
    @GetMapping("/audiences")
    public String audiences(Model model) {
        boolean enabled = audienceManager != null;
        model.addAttribute("audienceEnabled", enabled);
        if (enabled) {
            List<Map<String, Object>> audienceInfos = new ArrayList<>();
            for (Audience audience : audienceManager.listAudiences()) {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("name", audience.getName());
                info.put("tags", audience.getTags());
                info.put("contactCount", audienceManager.resolve(audience).size());
                audienceInfos.add(info);
            }
            model.addAttribute("audiences", audienceInfos);
            model.addAttribute("contacts", audienceManager.getContactRepository().findAll());
            model.addAttribute("contactCount", audienceManager.getContactRepository().count());
        }
        return "notify-admin/audiences";
    }

    /** Status Webhook -- configuration and recent deliveries. */
    @GetMapping("/status-webhook")
    public String statusWebhook(Model model) {
        boolean enabled = statusWebhookListener != null;
        model.addAttribute("webhookEnabled", enabled);
        if (enabled) {
            model.addAttribute("webhookUrl", statusWebhookListener.getUrl());
            model.addAttribute("webhookTimeoutMs", statusWebhookListener.getTimeoutMs());
            model.addAttribute("webhookHeaders", statusWebhookListener.getHeaders());
            model.addAttribute("recentDeliveries", statusWebhookListener.getRecentDeliveries());
        }
        return "notify-admin/status-webhook";
    }

    /** API: overview stats — count by status. */
    @GetMapping("/api/stats/overview")
    @ResponseBody
    public Map<String, Long> statsOverview() {
        NotificationTracker tracker = hub.getTracker();
        if (tracker == null) return Collections.emptyMap();
        Map<String, Long> result = new LinkedHashMap<>();
        tracker.countByStatus().forEach((status, count) -> result.put(status.name(), count));
        return result;
    }

    /** API: messages per channel. */
    @GetMapping("/api/stats/by-channel")
    @ResponseBody
    public Map<String, Long> statsByChannel() {
        NotificationTracker tracker = hub.getTracker();
        if (tracker == null) return Collections.emptyMap();
        return tracker.countByChannel();
    }

    /** API: timeline — sent/failed per day for the last N days. */
    @GetMapping("/api/stats/timeline")
    @ResponseBody
    public List<Map<String, Object>> statsTimeline(@RequestParam(name = "days", defaultValue = "7") int days) {
        NotificationTracker tracker = hub.getTracker();
        List<Map<String, Object>> timeline = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("date", date.toString());
            entry.put("sent", 0L);
            entry.put("failed", 0L);
            timeline.add(entry);
        }

        if (tracker != null) {
            for (DeliveryReceipt r : tracker.findAll()) {
                if (r.getTimestamp() == null) continue;
                LocalDate rDate = r.getTimestamp().atZone(ZoneId.systemDefault()).toLocalDate();
                for (Map<String, Object> entry : timeline) {
                    if (entry.get("date").equals(rDate.toString())) {
                        if (r.getStatus() == DeliveryStatus.SENT) {
                            entry.put("sent", (Long) entry.get("sent") + 1);
                        } else if (r.getStatus() == DeliveryStatus.FAILED) {
                            entry.put("failed", (Long) entry.get("failed") + 1);
                        }
                        break;
                    }
                }
            }
        }

        return timeline;
    }

    /** API: top error messages. */
    @GetMapping("/api/stats/errors")
    @ResponseBody
    public List<Map<String, Object>> statsErrors() {
        NotificationTracker tracker = hub.getTracker();
        if (tracker == null) return Collections.emptyList();

        Map<String, Long> errorCounts = tracker.findByStatus(DeliveryStatus.FAILED).stream()
                .filter(r -> r.getErrorMessage() != null && !r.getErrorMessage().isBlank())
                .collect(Collectors.groupingBy(DeliveryReceipt::getErrorMessage, Collectors.counting()));

        return errorCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("message", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
    }
}
