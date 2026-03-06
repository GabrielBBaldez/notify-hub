package io.notifyhub.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.notifyhub.core.Channel;
import io.notifyhub.core.DeliveryReceipt;
import io.notifyhub.core.Notifiable;
import io.notifyhub.core.NotifyHub;
import io.notifyhub.mcp.util.ToolResultHelper;

import java.util.Map;

public class SendPushTool {

    private final NotifyHub notifyHub;

    public SendPushTool(NotifyHub notifyHub) {
        this.notifyHub = notifyHub;
    }

    public SyncToolSpecification specification(McpJsonMapper jsonMapper) {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "push_token": {
                      "type": "string",
                      "description": "Firebase Cloud Messaging device token"
                    },
                    "subject": {
                      "type": "string",
                      "description": "Push notification title"
                    },
                    "body": {
                      "type": "string",
                      "description": "Push notification body"
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
                    "imageUrl": {
                      "type": "string",
                      "description": "Optional image URL to embed in the notification"
                    }
                  },
                  "required": ["push_token", "body"]
                }
                """;

        Tool tool = Tool.builder()
                .name("send_push")
                .description("Send a Firebase Cloud Messaging push notification.")
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
        String pushToken = (String) args.get("push_token");
        String subject = (String) args.get("subject");
        String body = (String) args.get("body");
        String template = (String) args.get("template");
        Map<String, Object> params = (Map<String, Object>) args.get("params");
        String imageUrl = (String) args.get("imageUrl");

        Notifiable pushRecipient = new Notifiable() {
            @Override
            public String getNotifyPushToken() {
                return pushToken;
            }
        };

        var builder = notifyHub.to(pushRecipient).via(Channel.PUSH);

        if (subject != null) builder.subject(subject);
        if (template != null) {
            builder.template(template);
            if (params != null) builder.params(params);
        } else {
            builder.content(body);
        }
        if (imageUrl != null) builder.image(imageUrl);

        DeliveryReceipt receipt = builder.sendTracked();

        return ToolResultHelper.success("Push notification sent", Map.of(
                "id", receipt.getId(),
                "channel", receipt.getChannelName(),
                "status", receipt.getStatus().name(),
                "timestamp", receipt.getTimestamp().toString()
        ));
    }
}
