package io.notifyhub.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.notifyhub.core.ScheduledNotification;
import io.notifyhub.mcp.util.ToolResultHelper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * MCP tool to cancel a pending scheduled notification by ID.
 */
public class CancelScheduledNotificationTool {

    private final ScheduledNotificationRegistry registry;

    public CancelScheduledNotificationTool(ScheduledNotificationRegistry registry) {
        this.registry = registry;
    }

    public SyncToolSpecification specification(McpJsonMapper jsonMapper) {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "id": {
                      "type": "string",
                      "description": "The ID of the scheduled notification to cancel (from schedule_notification or list_scheduled_notifications)"
                    }
                  },
                  "required": ["id"]
                }
                """;

        Tool tool = Tool.builder()
                .name("cancel_scheduled_notification")
                .description("Cancel a pending scheduled notification before it is sent. Use list_scheduled_notifications to find notification IDs.")
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
        String id = (String) args.get("id");
        if (id == null || id.isBlank()) {
            return ToolResultHelper.error("'id' must be provided");
        }

        Optional<ScheduledNotification> opt = registry.findById(id);
        if (opt.isEmpty()) {
            return ToolResultHelper.error("No scheduled notification found with ID: " + id);
        }

        ScheduledNotification sn = opt.get();

        if (sn.isCancelled()) {
            return ToolResultHelper.error("Notification " + id + " was already cancelled");
        }
        if (sn.isDone()) {
            return ToolResultHelper.error("Notification " + id + " has already been sent and cannot be cancelled");
        }

        boolean cancelled = sn.cancel();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", sn.getId());
        data.put("channel", sn.getChannelName());
        data.put("recipient", sn.getRecipient());
        data.put("scheduled_time", sn.getScheduledTime().toString());
        data.put("status", sn.getStatus().name());
        data.put("cancelled", cancelled);

        if (cancelled) {
            return ToolResultHelper.success("Scheduled notification cancelled successfully", data);
        } else {
            return ToolResultHelper.error("Failed to cancel notification " + id
                    + " — it may have already been dispatched");
        }
    }
}
