package io.notifyhub.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.notifyhub.core.NotifyHub;
import io.notifyhub.core.dlq.DeadLetter;
import io.notifyhub.core.dlq.DeadLetterQueue;
import io.notifyhub.mcp.util.ToolResultHelper;

import java.util.*;

public class ListDeadLettersTool {

    private final NotifyHub notifyHub;

    public ListDeadLettersTool(NotifyHub notifyHub) {
        this.notifyHub = notifyHub;
    }

    public SyncToolSpecification specification(McpJsonMapper jsonMapper) {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "channel": {
                      "type": "string",
                      "description": "Filter by channel name (e.g., \\"email\\", \\"sms\\")"
                    },
                    "limit": {
                      "type": "integer",
                      "description": "Maximum number of dead letters to return (default: 50)"
                    }
                  }
                }
                """;

        Tool tool = Tool.builder()
                .name("list_dead_letters")
                .description("List failed notifications in the dead letter queue (DLQ). These are notifications that failed after exhausting all retry attempts.")
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
        DeadLetterQueue dlq = notifyHub.getDeadLetterQueue();
        if (dlq == null) {
            return ToolResultHelper.error("Dead letter queue is not enabled. Set notify.dlq.enabled=true.");
        }

        String channelFilter = (String) args.get("channel");
        int limit = args.containsKey("limit") ? ((Number) args.get("limit")).intValue() : 50;

        List<DeadLetter> deadLetters;
        if (channelFilter != null && !channelFilter.isBlank()) {
            deadLetters = dlq.findByChannel(channelFilter);
        } else {
            deadLetters = dlq.findAll();
        }

        if (deadLetters.size() > limit) {
            deadLetters = deadLetters.subList(0, limit);
        }

        List<Map<String, Object>> results = new ArrayList<>();
        for (DeadLetter dl : deadLetters) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", dl.getId());
            entry.put("channel", dl.getChannelName());
            entry.put("recipient", dl.getRecipient());
            if (dl.getSubject() != null) entry.put("subject", dl.getSubject());
            entry.put("error", dl.getErrorMessage());
            entry.put("attempts", dl.getAttemptCount());
            entry.put("failedAt", dl.getFailedAt().toString());
            if (dl.getTemplateName() != null) entry.put("template", dl.getTemplateName());
            results.add(entry);
        }

        return ToolResultHelper.successJson(Map.of(
                "total", dlq.count(),
                "returned", results.size(),
                "deadLetters", results
        ));
    }
}
