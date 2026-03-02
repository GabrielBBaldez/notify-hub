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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SendYouTubeTool {

    private final NotifyHub notifyHub;

    public SendYouTubeTool(NotifyHub notifyHub) {
        this.notifyHub = notifyHub;
    }

    public SyncToolSpecification specification(McpJsonMapper jsonMapper) {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "recipient": {
                      "type": "string",
                      "description": "YouTube recipient alias (configured name) or liveChatId"
                    },
                    "body": {
                      "type": "string",
                      "description": "Live chat message content (max 200 characters)"
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
                    "poll_question": {
                      "type": "string",
                      "description": "Poll question text. If omitted, 'body' is used as the question."
                    },
                    "poll_options": {
                      "type": "array",
                      "items": { "type": "string" },
                      "minItems": 2,
                      "maxItems": 4,
                      "description": "Poll choices (2-4 options). When provided, creates a poll instead of a text message."
                    }
                  }
                }
                """;

        Tool tool = Tool.builder()
                .name("send_youtube")
                .description("Send a message or create a poll in a YouTube live chat via YouTube Data API v3.")
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
        String pollQuestion = (String) args.get("poll_question");
        List<String> pollOptions = args.get("poll_options") instanceof List<?> raw
                ? raw.stream().map(Object::toString).toList() : null;

        boolean isPoll = pollOptions != null && !pollOptions.isEmpty();

        if (!isPoll && body == null && template == null) {
            return ToolResultHelper.error("Either 'body', 'template', or 'poll_options' must be provided");
        }

        NotificationBuilder builder = notifyHub.to(recipient != null ? recipient : "default").via(Channel.YOUTUBE);

        if (isPoll) {
            // Poll mode: pass poll data via params
            Map<String, Object> pollParams = params != null ? new HashMap<>(params) : new HashMap<>();
            pollParams.put("poll_options", pollOptions);
            if (pollQuestion != null) pollParams.put("poll_question", pollQuestion);
            builder.params(pollParams);
            // Use body or poll_question as content (fallback for question text)
            builder.content(pollQuestion != null ? pollQuestion : (body != null ? body : "Poll"));
        } else if (template != null) {
            builder.template(template);
            if (params != null) builder.params(params);
        } else {
            builder.content(body);
        }

        DeliveryReceipt receipt = builder.sendTracked();

        String action = isPoll ? "YouTube poll created" : "YouTube live chat message sent";
        return ToolResultHelper.success(action, Map.of(
                "id", receipt.getId(),
                "channel", receipt.getChannelName(),
                "status", receipt.getStatus().name(),
                "timestamp", receipt.getTimestamp().toString()
        ));
    }
}
