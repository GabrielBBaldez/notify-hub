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

public class ListAudiencesTool {

    private final NotifyHub notifyHub;

    public ListAudiencesTool(NotifyHub notifyHub) {
        this.notifyHub = notifyHub;
    }

    public SyncToolSpecification specification(McpJsonMapper jsonMapper) {
        String schema = """
                {
                  "type": "object",
                  "properties": {},
                  "additionalProperties": false
                }
                """;

        Tool tool = Tool.builder()
                .name("list_audiences")
                .description("List all defined audiences with their tag filters and matching contact counts.")
                .inputSchema(jsonMapper, schema)
                .build();

        return new SyncToolSpecification(tool, (exchange, arguments) -> {
            try {
                return execute();
            } catch (Exception e) {
                return ToolResultHelper.error(e);
            }
        });
    }

    private CallToolResult execute() {
        AudienceManager audienceManager = notifyHub.getAudienceManager();
        if (audienceManager == null) {
            return ToolResultHelper.error("Audience management is not enabled. Set notify.audiences.enabled=true.");
        }

        List<Audience> audiences = audienceManager.listAudiences();
        List<Map<String, Object>> results = new ArrayList<>();

        for (Audience a : audiences) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", a.getName());
            entry.put("tags", a.getTags());
            entry.put("matchingContacts", audienceManager.resolve(a).size());
            results.add(entry);
        }

        return ToolResultHelper.successJson(Map.of(
                "total", results.size(),
                "audiences", results
        ));
    }
}
