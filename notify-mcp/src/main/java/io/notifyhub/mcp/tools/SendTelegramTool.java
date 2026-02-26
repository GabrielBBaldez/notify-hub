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

public class SendTelegramTool {

    private final NotifyHub notifyHub;

    public SendTelegramTool(NotifyHub notifyHub) {
        this.notifyHub = notifyHub;
    }

    public SyncToolSpecification specification(McpJsonMapper jsonMapper) {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "recipient": {
                      "type": "string",
                      "description": "Telegram chat ID or channel username (e.g., @mychannel, 123456789)"
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
                    }
                  },
                  "required": ["recipient"]
                }
                """;

        Tool tool = Tool.builder()
                .name("send_telegram")
                .description("Send a Telegram message via Bot API.")
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

        if (body == null && template == null) {
            return ToolResultHelper.error("Either 'body' or 'template' must be provided");
        }

        NotificationBuilder builder = notifyHub.to(recipient).via(Channel.TELEGRAM);

        if (template != null) {
            builder.template(template);
            if (params != null) builder.params(params);
        } else {
            builder.content(body);
        }

        DeliveryReceipt receipt = builder.sendTracked();

        return ToolResultHelper.success("Telegram message sent", Map.of(
                "id", receipt.getId(),
                "channel", receipt.getChannelName(),
                "recipient", receipt.getRecipient(),
                "status", receipt.getStatus().name(),
                "timestamp", receipt.getTimestamp().toString()
        ));
    }
}
