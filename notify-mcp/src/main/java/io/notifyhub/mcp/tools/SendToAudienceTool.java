package io.notifyhub.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.notifyhub.core.Channel;
import io.notifyhub.core.DeliveryReceipt;
import io.notifyhub.core.NotifyHub;
import io.notifyhub.core.Priority;
import io.notifyhub.mcp.util.ToolResultHelper;

import java.util.*;

public class SendToAudienceTool {

    private final NotifyHub notifyHub;

    public SendToAudienceTool(NotifyHub notifyHub) {
        this.notifyHub = notifyHub;
    }

    public SyncToolSpecification specification(McpJsonMapper jsonMapper) {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "audience": {
                      "type": "string",
                      "description": "Name of the audience to send to (must be created first with create_audience)"
                    },
                    "channel": {
                      "type": "string",
                      "description": "Channel to send through (e.g., \\"email\\", \\"sms\\", \\"slack\\")"
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
                    }
                  },
                  "required": ["audience", "channel"]
                }
                """;

        Tool tool = Tool.builder()
                .name("send_to_audience")
                .description("Send a notification to all contacts in a named audience. Each contact receives an individual tracked delivery.")
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
        String audienceName = (String) args.get("audience");
        String channel = (String) args.get("channel");
        String subject = (String) args.get("subject");
        String body = (String) args.get("body");
        String template = (String) args.get("template");
        Map<String, Object> params = (Map<String, Object>) args.get("params");
        String priority = (String) args.get("priority");

        if (body == null && template == null) {
            return ToolResultHelper.error("Either 'body' or 'template' must be provided");
        }

        var builder = notifyHub.toAudience(audienceName)
                .via(Channel.custom(channel));

        if (subject != null) builder.subject(subject);
        if (template != null) {
            builder.template(template);
            if (params != null) builder.params(params);
        } else {
            builder.content(body);
        }
        if (priority != null) builder.priority(Priority.valueOf(priority));

        List<DeliveryReceipt> receipts = builder.send();

        long sent = receipts.stream().filter(r -> r.getStatus().name().equals("SENT")).count();
        long failed = receipts.stream().filter(r -> r.getStatus().name().equals("FAILED")).count();

        List<Map<String, String>> results = new ArrayList<>();
        for (DeliveryReceipt r : receipts) {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("id", r.getId());
            entry.put("recipient", r.getRecipient());
            entry.put("status", r.getStatus().name());
            if (r.getErrorMessage() != null) entry.put("error", r.getErrorMessage());
            results.add(entry);
        }

        return ToolResultHelper.success(
                "Sent to audience '" + audienceName + "': " + sent + " delivered, " + failed + " failed",
                Map.of("receipts", results)
        );
    }
}
