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

public class SendBatchTool {

    private final NotifyHub notifyHub;

    public SendBatchTool(NotifyHub notifyHub) {
        this.notifyHub = notifyHub;
    }

    public SyncToolSpecification specification(McpJsonMapper jsonMapper) {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "recipients": {
                      "type": "array",
                      "items": { "type": "string" },
                      "description": "List of recipient addresses (emails, phone numbers, or channel-specific IDs)"
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
                    },
                    "imageUrl": {
                      "type": "string",
                      "description": "Optional image URL to embed in the notification"
                    }
                  },
                  "required": ["recipients", "channel"]
                }
                """;

        Tool tool = Tool.builder()
                .name("send_batch")
                .description("Send the same notification to multiple recipients at once. Each recipient gets an individual tracked delivery. Failures for one recipient do not prevent delivery to others.")
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
        List<String> recipients = (List<String>) args.get("recipients");
        String channel = (String) args.get("channel");
        String subject = (String) args.get("subject");
        String body = (String) args.get("body");
        String template = (String) args.get("template");
        Map<String, Object> params = (Map<String, Object>) args.get("params");
        String priority = (String) args.get("priority");
        String imageUrl = (String) args.get("imageUrl");

        if (body == null && template == null) {
            return ToolResultHelper.error("Either 'body' or 'template' must be provided");
        }
        if (recipients == null || recipients.isEmpty()) {
            return ToolResultHelper.error("At least one recipient must be specified");
        }

        var builder = notifyHub.toAll(recipients)
                .via(Channel.custom(channel));

        if (subject != null) builder.subject(subject);
        if (template != null) {
            builder.template(template);
            if (params != null) builder.params(params);
        } else {
            builder.content(body);
        }
        if (priority != null) builder.priority(Priority.valueOf(priority));
        if (imageUrl != null) builder.image(imageUrl);

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
                "Batch sent to " + recipients.size() + " recipients: " + sent + " delivered, " + failed + " failed",
                Map.of("receipts", results)
        );
    }
}
