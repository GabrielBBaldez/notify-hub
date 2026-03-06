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

import java.util.Map;

public class SendNotificationTool {

    private final NotifyHub notifyHub;

    public SendNotificationTool(NotifyHub notifyHub) {
        this.notifyHub = notifyHub;
    }

    public SyncToolSpecification specification(McpJsonMapper jsonMapper) {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "channel": {
                      "type": "string",
                      "description": "Notification channel to use",
                      "enum": ["email", "sms", "slack", "telegram", "discord", "teams", "whatsapp", "push", "websocket", "google-chat"]
                    },
                    "recipient": {
                      "type": "string",
                      "description": "Recipient address (email, phone number, channel ID, or push token depending on channel)"
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
                  "required": ["channel", "recipient"]
                }
                """;

        Tool tool = Tool.builder()
                .name("send_notification")
                .description("Send a notification through any configured NotifyHub channel. Provide either 'body' for direct content or 'template' with 'params' for templated content.")
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
        String channelName = (String) args.get("channel");
        String recipient = (String) args.get("recipient");
        String subject = (String) args.get("subject");
        String body = (String) args.get("body");
        String template = (String) args.get("template");
        Map<String, Object> params = (Map<String, Object>) args.get("params");
        String priority = (String) args.get("priority");
        String imageUrl = (String) args.get("imageUrl");

        if (body == null && template == null) {
            return ToolResultHelper.error("Either 'body' or 'template' must be provided");
        }

        NotificationBuilder builder;
        if ("sms".equals(channelName) || "whatsapp".equals(channelName)) {
            builder = notifyHub.toPhone(recipient);
        } else {
            builder = notifyHub.to(recipient);
        }

        builder.via(Channel.custom(channelName));

        if (subject != null) builder.subject(subject);
        if (template != null) {
            builder.template(template);
            if (params != null) builder.params(params);
        } else {
            builder.content(body);
        }
        if (priority != null) builder.priority(Priority.valueOf(priority));
        if (imageUrl != null) builder.image(imageUrl);

        DeliveryReceipt receipt = builder.sendTracked();

        return ToolResultHelper.success("Notification sent via " + channelName, Map.of(
                "id", receipt.getId(),
                "channel", receipt.getChannelName(),
                "recipient", receipt.getRecipient(),
                "status", receipt.getStatus().name(),
                "timestamp", receipt.getTimestamp().toString()
        ));
    }
}
