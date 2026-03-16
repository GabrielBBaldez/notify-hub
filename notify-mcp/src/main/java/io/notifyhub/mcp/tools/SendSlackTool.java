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

public class SendSlackTool {

    private final NotifyHub notifyHub;

    public SendSlackTool(NotifyHub notifyHub) {
        this.notifyHub = notifyHub;
    }

    public SyncToolSpecification specification(McpJsonMapper jsonMapper) {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "recipient": {
                      "type": "string",
                      "description": "Slack recipient alias (configured name), webhook URL, or channel/user ID (e.g., #general, @user)"
                    },
                    "body": {
                      "type": "string",
                      "description": "Message content"
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
                    "sender_name": {
                      "type": "string",
                      "description": "Override bot display name for this message"
                    },
                    "sender_avatar": {
                      "type": "string",
                      "description": "Override bot avatar URL for this message"
                    }
                  },
                  "required": ["recipient"]
                }
                """;

        Tool tool = Tool.builder()
                .name("send_slack")
                .description("Send a Slack message via webhook.")
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
        String senderName = (String) args.get("sender_name");
        String senderAvatar = (String) args.get("sender_avatar");

        if (body == null && template == null) {
            return ToolResultHelper.error("Either 'body' or 'template' must be provided");
        }

        NotificationBuilder builder = notifyHub.to(recipient).via(Channel.SLACK);

        if (template != null) {
            builder.template(template);
            if (params != null) builder.params(params);
        } else {
            builder.content(body);
        }

        if (senderName != null && !senderName.isEmpty()) {
            builder.param("senderName", senderName);
        }
        if (senderAvatar != null && !senderAvatar.isEmpty()) {
            builder.param("senderAvatar", senderAvatar);
        }

        DeliveryReceipt receipt = builder.sendTracked();

        return ToolResultHelper.success("Slack message sent", Map.of(
                "id", receipt.getId(),
                "channel", receipt.getChannelName(),
                "recipient", receipt.getRecipient(),
                "status", receipt.getStatus().name(),
                "timestamp", receipt.getTimestamp().toString()
        ));
    }
}
