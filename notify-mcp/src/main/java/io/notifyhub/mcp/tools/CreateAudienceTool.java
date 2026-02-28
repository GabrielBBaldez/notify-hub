package io.notifyhub.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.notifyhub.core.NotifyHub;
import io.notifyhub.core.audience.Audience;
import io.notifyhub.core.audience.AudienceManager;
import io.notifyhub.mcp.util.ToolResultHelper;

import java.util.*;

public class CreateAudienceTool {

    private final NotifyHub notifyHub;

    public CreateAudienceTool(NotifyHub notifyHub) {
        this.notifyHub = notifyHub;
    }

    public SyncToolSpecification specification(McpJsonMapper jsonMapper) {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "name": {
                      "type": "string",
                      "description": "Audience name (e.g., \\"premium-users\\", \\"newsletter\\")"
                    },
                    "tags": {
                      "type": "array",
                      "items": { "type": "string" },
                      "description": "Tags that contacts must have to be included (AND logic). E.g., [\\"vip\\", \\"plan:premium\\"] matches contacts with BOTH tags."
                    }
                  },
                  "required": ["name", "tags"]
                }
                """;

        Tool tool = Tool.builder()
                .name("create_audience")
                .description("Create a named audience defined by tag filters. Contacts matching ALL tags are included. Use with send_to_audience to send batch notifications.")
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
        AudienceManager audienceManager = notifyHub.getAudienceManager();
        if (audienceManager == null) {
            return ToolResultHelper.error("Audience management is not enabled. Set notify.audiences.enabled=true.");
        }

        String name = (String) args.get("name");
        List<String> tags = (List<String>) args.get("tags");

        if (tags == null || tags.isEmpty()) {
            return ToolResultHelper.error("At least one tag must be specified");
        }

        Audience audience = audienceManager.createAudience(name, new LinkedHashSet<>(tags));
        int matchingContacts = audienceManager.resolve(audience).size();

        return ToolResultHelper.success("Audience '" + name + "' created", Map.of(
                "name", audience.getName(),
                "tags", audience.getTags(),
                "matchingContacts", matchingContacts
        ));
    }
}
