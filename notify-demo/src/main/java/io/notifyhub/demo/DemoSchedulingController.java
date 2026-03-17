package io.notifyhub.demo;

import io.notifyhub.core.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scheduling-related demo endpoints: schedule, list, cancel notifications.
 */
@RestController
@Tag(name = "Scheduling", description = "Schedule, list, and cancel notifications")
public class DemoSchedulingController {

    private final NotifyHub notify;

    /** Stores scheduled notifications by ID for inspection */
    private final Map<String, ScheduledNotification> scheduledMap = new ConcurrentHashMap<>();

    public DemoSchedulingController(NotifyHub notify) {
        this.notify = notify;
    }

    /** Expose scheduledMap so DemoController can clear it from /inbox DELETE */
    Map<String, ScheduledNotification> getScheduledMap() {
        return scheduledMap;
    }

    // ===================== SCHEDULED NOTIFICATION =====================

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

    @Operation(summary = "List scheduled", description = "List all scheduled notifications and their status")
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

    @Operation(summary = "Cancel scheduled", description = "Cancel a scheduled notification by ID")
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
}
