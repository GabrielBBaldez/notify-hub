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

public class SendWhatsAppTool {

    private final NotifyHub notifyHub;

    public SendWhatsAppTool(NotifyHub notifyHub) {
        this.notifyHub = notifyHub;
    }

    public SyncToolSpecification specification(McpJsonMapper jsonMapper) {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "phone": {
                      "type": "string",
                      "description": "Recipient phone number in E.164 format (e.g., +5548999999999)"
                    },
                    "body": {
                      "type": "string",
                      "description": "WhatsApp message content"
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
                    },
                    "media_url": {
                      "type": "string",
                      "description": "Public URL of media to attach (image, video, PDF). Twilio fetches this URL to send with the message."
                    }
                  },
                  "required": ["phone"]
                }
                """;

        Tool tool = Tool.builder()
                .name("send_whatsapp")
                .description("Send a WhatsApp message via Twilio or WhatsApp Cloud API.")
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
        String phone = (String) args.get("phone");
        String body = (String) args.get("body");
        String template = (String) args.get("template");
        Map<String, Object> params = (Map<String, Object>) args.get("params");
        String imageUrl = (String) args.get("imageUrl");
        String mediaUrl = (String) args.get("media_url");

        if (body == null && template == null) {
            return ToolResultHelper.error("Either 'body' or 'template' must be provided");
        }

        NotificationBuilder builder = notifyHub.toPhone(phone).via(Channel.WHATSAPP);

        if (template != null) {
            builder.template(template);
            if (params != null) builder.params(params);
        } else {
            builder.content(body);
        }
        if (imageUrl != null) builder.image(imageUrl);

        if (mediaUrl != null && !mediaUrl.isEmpty()) {
            builder.param("mediaUrl", mediaUrl);
        }

        DeliveryReceipt receipt = builder.sendTracked();

        return ToolResultHelper.success("WhatsApp message sent to " + phone, Map.of(
                "id", receipt.getId(),
                "channel", receipt.getChannelName(),
                "recipient", receipt.getRecipient(),
                "status", receipt.getStatus().name(),
                "timestamp", receipt.getTimestamp().toString()
        ));
    }
}
