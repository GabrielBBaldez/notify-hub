package io.notifyhub.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.notifyhub.core.ScheduledNotification;
import io.notifyhub.mcp.util.ToolResultHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MCP tool to list scheduled notifications with optional filters.
 */
public class ListScheduledNotificationsTool {

    private final ScheduledNotificationRegistry registry;

    public ListScheduledNotificationsTool(ScheduledNotificationRegistry registry) {
        this.registry = registry;
    }

    public SyncToolSpecification specification(McpJsonMapper jsonMapper) {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "status": {
                      "type": "string",
                      "description": "Filter by status",
                      "enum": ["SCHEDULED", "SENT", "FAILED", "CANCELLED"]
                    },
                    "channel": {
                      "type": "string",
                      "description": "Filter by channel name (e.g., 'email', 'slack')"
                    },
                    "limit": {
                      "type": "integer",
                      "description": "Maximum number of results to return (default: 50)"
                    }
                  }
                }
                """;

        Tool tool = Tool.builder()
                .name("list_scheduled_notifications")
                .description("List scheduled notifications with their current state. Supports filtering by status and channel.")
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

    private CallToolResult execute(Map<String, Object> args) {
        registry.evictStale();

        String statusFilter = (String) args.get("status");
        String channelFilter = (String) args.get("channel");
        int limit = args.containsKey("limit") ? ((Number) args.get("limit")).intValue() : 50;

        List<ScheduledNotification> all = registry.findAll();

        List<ScheduledNotification> filtered = all.stream()
                .filter(sn -> statusFilter == null || sn.getStatus().name().equals(statusFilter))
                .filter(sn -> channelFilter == null || sn.getChannelName().equals(channelFilter))
                .sorted(Comparator.comparing(ScheduledNotification::getScheduledTime))
                .limit(limit)
                .collect(Collectors.toList());

        List<Map<String, Object>> results = new ArrayList<>();
        for (ScheduledNotification sn : filtered) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", sn.getId());
            entry.put("channel", sn.getChannelName());
            entry.put("recipient", sn.getRecipient());
            entry.put("scheduled_time", sn.getScheduledTime().toString());
            entry.put("status", sn.getStatus().name());
            entry.put("remaining_delay", ScheduleNotificationTool.formatDuration(sn.getRemainingDelay()));
            entry.put("done", sn.isDone());
            entry.put("cancelled", sn.isCancelled());
            results.add(entry);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", all.size());
        response.put("returned", results.size());
        response.put("scheduled_notifications", results);

        return ToolResultHelper.successJson(response);
    }
}
