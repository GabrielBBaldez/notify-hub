package io.notifyhub.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.notifyhub.core.Channel;
import io.notifyhub.core.DeliveryReceipt;
import io.notifyhub.core.NotificationBuilder;
import io.notifyhub.core.NotifyHub;
import io.notifyhub.core.Priority;
import io.notifyhub.mcp.util.ToolResultHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SendMultiChannelTool {

    private final NotifyHub notifyHub;

    public SendMultiChannelTool(NotifyHub notifyHub) {
        this.notifyHub = notifyHub;
    }

    public SyncToolSpecification specification(McpJsonMapper jsonMapper) {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "channels": {
                      "type": "array",
                      "items": { "type": "string" },
                      "description": "List of channels to send through simultaneously (e.g., [\\"email\\", \\"slack\\", \\"telegram\\"])"
                    },
                    "recipient": {
                      "type": "string",
                      "description": "Recipient address (email, phone, or channel ID)"
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
                  "required": ["channels", "recipient"]
                }
                """;

        Tool tool = Tool.builder()
                .name("send_multi_channel")
                .description("Send a notification to multiple channels simultaneously. All channels receive the same message.")
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
        List<String> channels = (List<String>) args.get("channels");
        String recipient = (String) args.get("recipient");
        String subject = (String) args.get("subject");
        String body = (String) args.get("body");
        String template = (String) args.get("template");
        Map<String, Object> params = (Map<String, Object>) args.get("params");
        String priority = (String) args.get("priority");

        if (body == null && template == null) {
            return ToolResultHelper.error("Either 'body' or 'template' must be provided");
        }
        if (channels == null || channels.isEmpty()) {
            return ToolResultHelper.error("At least one channel must be specified");
        }

        NotificationBuilder builder = notifyHub.to(recipient);

        for (String ch : channels) {
            builder.via(Channel.custom(ch));
        }

        if (subject != null) builder.subject(subject);
        if (template != null) {
            builder.template(template);
            if (params != null) builder.params(params);
        } else {
            builder.content(body);
        }
        if (priority != null) builder.priority(Priority.valueOf(priority));

        List<DeliveryReceipt> receipts = builder.sendAllTracked();

        List<Map<String, String>> results = new ArrayList<>();
        for (DeliveryReceipt r : receipts) {
            results.add(Map.of(
                    "id", r.getId(),
                    "channel", r.getChannelName(),
                    "status", r.getStatus().name()
            ));
        }

        return ToolResultHelper.success(
                "Notification sent to " + channels.size() + " channels",
                Map.of("receipts", results)
        );
    }
}
