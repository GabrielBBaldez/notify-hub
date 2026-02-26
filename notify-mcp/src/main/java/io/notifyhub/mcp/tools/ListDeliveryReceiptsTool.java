package io.notifyhub.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.notifyhub.core.DeliveryReceipt;
import io.notifyhub.core.DeliveryStatus;
import io.notifyhub.core.NotificationTracker;
import io.notifyhub.core.NotifyHub;
import io.notifyhub.mcp.util.ToolResultHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ListDeliveryReceiptsTool {

    private final NotifyHub notifyHub;

    public ListDeliveryReceiptsTool(NotifyHub notifyHub) {
        this.notifyHub = notifyHub;
    }

    public SyncToolSpecification specification(McpJsonMapper jsonMapper) {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "status": {
                      "type": "string",
                      "description": "Filter by delivery status",
                      "enum": ["PENDING", "SCHEDULED", "SENT", "FAILED", "CANCELLED"]
                    },
                    "recipient": {
                      "type": "string",
                      "description": "Filter by recipient address"
                    },
                    "limit": {
                      "type": "integer",
                      "description": "Maximum number of receipts to return (default: 50)"
                    }
                  }
                }
                """;

        Tool tool = Tool.builder()
                .name("list_delivery_receipts")
                .description("List delivery receipts (notification history) with optional filters by status and recipient.")
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
        NotificationTracker tracker = notifyHub.getTracker();
        if (tracker == null) {
            return ToolResultHelper.error("Delivery tracking is not enabled. Set notify.tracking.enabled=true.");
        }

        String statusFilter = (String) args.get("status");
        String recipientFilter = (String) args.get("recipient");
        int limit = args.containsKey("limit") ? ((Number) args.get("limit")).intValue() : 50;

        List<DeliveryReceipt> receipts;

        if (statusFilter != null) {
            receipts = tracker.findByStatus(DeliveryStatus.valueOf(statusFilter));
        } else if (recipientFilter != null) {
            receipts = tracker.findByRecipient(recipientFilter);
        } else {
            receipts = tracker.findAll();
        }

        if (receipts.size() > limit) {
            receipts = receipts.subList(0, limit);
        }

        List<Map<String, Object>> results = new ArrayList<>();
        for (DeliveryReceipt r : receipts) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", r.getId());
            entry.put("channel", r.getChannelName());
            entry.put("recipient", r.getRecipient());
            entry.put("status", r.getStatus().name());
            entry.put("timestamp", r.getTimestamp().toString());
            if (r.getErrorMessage() != null) {
                entry.put("error", r.getErrorMessage());
            }
            results.add(entry);
        }

        return ToolResultHelper.successJson(Map.of(
                "total", tracker.count(),
                "returned", results.size(),
                "receipts", results
        ));
    }
}
