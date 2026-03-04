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

public class SendEmailTool {

    private final NotifyHub notifyHub;

    public SendEmailTool(NotifyHub notifyHub) {
        this.notifyHub = notifyHub;
    }

    public SyncToolSpecification specification(McpJsonMapper jsonMapper) {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "to": {
                      "type": "string",
                      "description": "Recipient email address"
                    },
                    "subject": {
                      "type": "string",
                      "description": "Email subject line"
                    },
                    "body": {
                      "type": "string",
                      "description": "Email body content (HTML or plain text)"
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
                  "required": ["to"]
                }
                """;

        Tool tool = Tool.builder()
                .name("send_email")
                .description("Send an email notification via SMTP or SendGrid (whichever is configured). " +
                        "When SendGrid is configured, returns a provider_message_id for delivery tracking.")
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
        String to = (String) args.get("to");
        String subject = (String) args.get("subject");
        String body = (String) args.get("body");
        String template = (String) args.get("template");
        Map<String, Object> params = (Map<String, Object>) args.get("params");
        String priority = (String) args.get("priority");

        if (body == null && template == null) {
            return ToolResultHelper.error("Either 'body' or 'template' must be provided");
        }

        NotificationBuilder builder = notifyHub.to(to).via(Channel.EMAIL);

        if (subject != null) builder.subject(subject);
        if (template != null) {
            builder.template(template);
            if (params != null) builder.params(params);
        } else {
            builder.content(body);
        }
        if (priority != null) builder.priority(Priority.valueOf(priority));

        DeliveryReceipt receipt = builder.sendTracked();

        java.util.LinkedHashMap<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("id", receipt.getId());
        data.put("channel", receipt.getChannelName());
        data.put("recipient", receipt.getRecipient());
        data.put("status", receipt.getStatus().name());
        data.put("timestamp", receipt.getTimestamp().toString());
        if (receipt.getProviderMessageId() != null) {
            data.put("provider_message_id", receipt.getProviderMessageId());
            data.put("tracking_supported", true);
        }

        return ToolResultHelper.success("Email sent to " + to, data);
    }
}
