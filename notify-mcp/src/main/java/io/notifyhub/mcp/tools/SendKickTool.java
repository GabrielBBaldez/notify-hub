package io.notifyhub.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.notifyhub.core.Channel;
import io.notifyhub.core.DeliveryReceipt;
import io.notifyhub.core.NotificationBuilder;
import io.notifyhub.core.NotifyHub;
import io.notifyhub.mcp.util.ToolResultHelper;

import java.util.Map;

public class SendKickTool {

    private final NotifyHub notifyHub;

    public SendKickTool(NotifyHub notifyHub) {
        this.notifyHub = notifyHub;
    }

    public SyncToolSpecification specification(McpJsonMapper jsonMapper) {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "recipient": {
                      "type": "string",
                      "description": "Kick recipient alias (configured name) or broadcaster ID"
                    },
                    "body": {
                      "type": "string",
                      "description": "Chat message content (max 500 characters)"
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
                    "messageType": {
                      "type": "string",
                      "description": "Message type: 'bot' (default) or 'user'",
                      "enum": ["bot", "user"]
                    }
                  },
                  "required": ["body"]
                }
                """;

        Tool tool = Tool.builder()
                .name("send_kick")
                .description("Send a chat message to a Kick channel via Kick Public API.")
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
        String recipient = (String) args.get("recipient");
        String body = (String) args.get("body");
        String template = (String) args.get("template");
        Map<String, Object> params = (Map<String, Object>) args.get("params");
        String messageType = (String) args.get("messageType");

        if (body == null && template == null) {
            return ToolResultHelper.error("Either 'body' or 'template' must be provided");
        }

        NotificationBuilder builder = notifyHub.to(recipient != null ? recipient : "default").via(Channel.KICK);

        if (template != null) {
            builder.template(template);
            if (params != null) builder.params(params);
        } else {
            builder.content(body);
        }

        if (messageType != null) {
            builder.param("messageType", messageType);
        }

        DeliveryReceipt receipt = builder.sendTracked();

        return ToolResultHelper.success("Kick message sent", Map.of(
                "id", receipt.getId(),
                "channel", receipt.getChannelName(),
                "status", receipt.getStatus().name(),
                "timestamp", receipt.getTimestamp().toString()
        ));
    }
}
