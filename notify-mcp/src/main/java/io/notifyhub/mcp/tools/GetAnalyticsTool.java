package io.notifyhub.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.notifyhub.core.DeliveryStatus;
import io.notifyhub.core.NotificationTracker;
import io.notifyhub.core.NotifyHub;
import io.notifyhub.core.dlq.DeadLetterQueue;
import io.notifyhub.mcp.util.ToolResultHelper;

import java.util.*;

public class GetAnalyticsTool {

    private final NotifyHub notifyHub;

    public GetAnalyticsTool(NotifyHub notifyHub) {
        this.notifyHub = notifyHub;
    }

    public SyncToolSpecification specification(McpJsonMapper jsonMapper) {
        String schema = """
                {
                  "type": "object",
                  "properties": {},
                  "additionalProperties": false
                }
                """;

        Tool tool = Tool.builder()
                .name("get_analytics")
                .description("Get notification delivery analytics: total counts, breakdown by channel and status, dead letter queue size, and active channels.")
                .inputSchema(jsonMapper, schema)
                .build();

        return new SyncToolSpecification(tool, (exchange, arguments) -> {
            try {
                return execute();
            } catch (Exception e) {
                return ToolResultHelper.error(e);
            }
        });
    }

    private CallToolResult execute() {
        Map<String, Object> analytics = new LinkedHashMap<>();

        // Tracking stats
        NotificationTracker tracker = notifyHub.getTracker();
        if (tracker != null) {
            analytics.put("totalNotifications", tracker.count());

            // By status
            Map<DeliveryStatus, Long> byStatus = tracker.countByStatus();
            Map<String, Long> statusMap = new LinkedHashMap<>();
            for (Map.Entry<DeliveryStatus, Long> e : byStatus.entrySet()) {
                statusMap.put(e.getKey().name().toLowerCase(), e.getValue());
            }
            analytics.put("byStatus", statusMap);

            // By channel
            Map<String, Long> byChannel = tracker.countByChannel();
            analytics.put("byChannel", byChannel);
        } else {
            analytics.put("tracking", "disabled");
        }

        // DLQ stats
        DeadLetterQueue dlq = notifyHub.getDeadLetterQueue();
        if (dlq != null) {
            analytics.put("deadLetterCount", dlq.count());
        } else {
            analytics.put("dlq", "disabled");
        }

        // Active channels
        Set<String> channels = notifyHub.getRegisteredChannels();
        analytics.put("activeChannels", channels.size());
        analytics.put("channels", channels);

        // Audience stats
        if (notifyHub.getAudienceManager() != null) {
            analytics.put("audiences", notifyHub.getAudienceManager().listAudiences().size());
            analytics.put("contacts", notifyHub.getAudienceManager().getContactRepository().count());
        }

        return ToolResultHelper.successJson(analytics);
    }
}
