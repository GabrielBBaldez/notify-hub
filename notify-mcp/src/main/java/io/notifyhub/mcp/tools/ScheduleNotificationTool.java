package io.notifyhub.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.notifyhub.core.Channel;
import io.notifyhub.core.NotificationBuilder;
import io.notifyhub.core.NotifyHub;
import io.notifyhub.core.Priority;
import io.notifyhub.core.ScheduledNotification;
import io.notifyhub.mcp.util.ToolResultHelper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MCP tool to schedule a notification for future delivery.
 * Supports relative delay ("5m", "1h30m") or absolute datetime with timezone.
 */
public class ScheduleNotificationTool {

    private static final Pattern DELAY_PATTERN =
            Pattern.compile("(?:(\\d+)d)?\\s*(?:(\\d+)h)?\\s*(?:(\\d+)m)?\\s*(?:(\\d+)s)?");

    private final NotifyHub notifyHub;
    private final ScheduledNotificationRegistry registry;

    public ScheduleNotificationTool(NotifyHub notifyHub, ScheduledNotificationRegistry registry) {
        this.notifyHub = notifyHub;
        this.registry = registry;
    }

    public SyncToolSpecification specification(McpJsonMapper jsonMapper) {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "channel": {
                      "type": "string",
                      "description": "Notification channel to use (e.g., 'email', 'sms', 'slack', 'telegram')"
                    },
                    "recipient": {
                      "type": "string",
                      "description": "Recipient address (email, phone number, channel ID, or push token depending on channel)"
                    },
                    "subject": {
                      "type": "string",
                      "description": "Subject line (primarily for email)"
                    },
                    "body": {
                      "type": "string",
                      "description": "Message body content"
                    },
                    "template": {
                      "type": "string",
                      "description": "Template name to use instead of body"
                    },
                    "params": {
                      "type": "object",
                      "description": "Template parameters as key-value pairs",
                      "additionalProperties": true
                    },
                    "priority": {
                      "type": "string",
                      "description": "Notification priority",
                      "enum": ["URGENT", "HIGH", "NORMAL", "LOW"]
                    },
                    "delay": {
                      "type": "string",
                      "description": "Relative delay before sending (e.g., '5m', '1h30m', '2d', '90s'). Mutually exclusive with scheduled_at."
                    },
                    "scheduled_at": {
                      "type": "string",
                      "description": "Absolute ISO-8601 date-time to send at (e.g., '2026-03-08T09:00:00'). Mutually exclusive with delay."
                    },
                    "timezone": {
                      "type": "string",
                      "description": "IANA timezone for scheduled_at (e.g., 'America/Sao_Paulo'). Defaults to server timezone if omitted."
                    }
                  },
                  "required": ["channel", "recipient"]
                }
                """;

        Tool tool = Tool.builder()
                .name("schedule_notification")
                .description("Schedule a notification for future delivery. Use 'delay' for relative timing (e.g., '5m', '1h') or 'scheduled_at' for a specific date-time. Returns a scheduled notification ID that can be used with list_scheduled_notifications and cancel_scheduled_notification.")
                .inputSchema(jsonMapper, schema)
                .build();

        return new SyncToolSpecification(tool, (exchange, arguments) -> {
            try {
                return execute(arguments);
            } catch (Exception e) {
                return ToolResultHelper.error(e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private CallToolResult execute(Map<String, Object> args) {
        String channelName = (String) args.get("channel");
        if (channelName == null || channelName.isBlank()) {
            return ToolResultHelper.error("'channel' must be provided");
        }
        String recipient = (String) args.get("recipient");
        if (recipient == null || recipient.isBlank()) {
            return ToolResultHelper.error("'recipient' must be provided");
        }
        String subject = (String) args.get("subject");
        String body = (String) args.get("body");
        String template = (String) args.get("template");
        Map<String, Object> params = (Map<String, Object>) args.get("params");
        String priority = (String) args.get("priority");
        String delay = (String) args.get("delay");
        String scheduledAt = (String) args.get("scheduled_at");
        String timezone = (String) args.get("timezone");

        if (body == null && template == null) {
            return ToolResultHelper.error("Either 'body' or 'template' must be provided");
        }
        if (delay == null && scheduledAt == null) {
            return ToolResultHelper.error("Either 'delay' or 'scheduled_at' must be provided");
        }
        if (delay != null && scheduledAt != null) {
            return ToolResultHelper.error("Provide either 'delay' or 'scheduled_at', not both");
        }

        // Build notification (same pattern as SendNotificationTool)
        NotificationBuilder builder;
        if ("sms".equals(channelName) || "whatsapp".equals(channelName)) {
            builder = notifyHub.toPhone(recipient);
        } else {
            builder = notifyHub.to(recipient);
        }

        builder.via(Channel.custom(channelName));

        if (subject != null) builder.subject(subject);
        if (template != null) {
            builder.template(template);
            if (params != null) builder.params(params);
        } else {
            builder.content(body);
        }
        if (priority != null) builder.priority(Priority.valueOf(priority));

        // Schedule
        ScheduledNotification scheduled;
        if (delay != null) {
            Duration duration = parseDelay(delay);
            scheduled = builder.schedule(duration);
        } else {
            LocalDateTime dateTime = LocalDateTime.parse(scheduledAt);
            ZoneId zoneId = timezone != null ? ZoneId.of(timezone) : ZoneId.systemDefault();
            scheduled = builder.scheduleAt(dateTime, zoneId);
        }

        // Store in registry
        registry.register(scheduled);

        // Build response
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", scheduled.getId());
        data.put("channel", scheduled.getChannelName());
        data.put("recipient", scheduled.getRecipient());
        data.put("scheduled_time", scheduled.getScheduledTime().toString());
        data.put("remaining_delay", formatDuration(scheduled.getRemainingDelay()));
        data.put("status", scheduled.getStatus().name());

        return ToolResultHelper.success(
                "Notification scheduled via " + channelName + " for " + scheduled.getScheduledTime(),
                data
        );
    }

    /**
     * Parse human-readable delay strings like "5m", "1h30m", "2d", "90s".
     * Falls back to ISO-8601 duration format ("PT5M").
     */
    static Duration parseDelay(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Delay must not be empty");
        }

        String normalized = input.trim().toLowerCase();

        // Try human-readable format: "5m", "1h", "1h30m", "2d", "90s"
        Matcher m = DELAY_PATTERN.matcher(normalized);
        if (m.matches() && !m.group(0).isEmpty()) {
            long days = m.group(1) != null ? Long.parseLong(m.group(1)) : 0;
            long hours = m.group(2) != null ? Long.parseLong(m.group(2)) : 0;
            long minutes = m.group(3) != null ? Long.parseLong(m.group(3)) : 0;
            long seconds = m.group(4) != null ? Long.parseLong(m.group(4)) : 0;

            if (days == 0 && hours == 0 && minutes == 0 && seconds == 0) {
                throw new IllegalArgumentException(
                        "Cannot parse delay: '" + input + "'. Use formats like '5m', '1h30m', '2d', or '90s'");
            }

            return Duration.ofDays(days).plusHours(hours).plusMinutes(minutes).plusSeconds(seconds);
        }

        // Fallback: try ISO-8601 duration ("PT5M", "PT1H30M")
        try {
            return Duration.parse(normalized.startsWith("pt") ? normalized.toUpperCase() : "PT" + normalized.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Cannot parse delay: '" + input + "'. Use formats like '5m', '1h30m', '2d', or ISO-8601 'PT5M'");
        }
    }

    static String formatDuration(Duration d) {
        if (d.isZero()) return "0s";
        long totalSeconds = d.getSeconds();
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d");
        if (hours > 0) sb.append(hours).append("h");
        if (minutes > 0) sb.append(minutes).append("m");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");
        return sb.toString();
    }
}
