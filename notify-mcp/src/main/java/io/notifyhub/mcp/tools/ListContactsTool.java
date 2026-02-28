package io.notifyhub.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.notifyhub.core.NotifyHub;
import io.notifyhub.core.audience.AudienceManager;
import io.notifyhub.core.audience.Contact;
import io.notifyhub.core.audience.ContactRepository;
import io.notifyhub.mcp.util.ToolResultHelper;

import java.util.*;

public class ListContactsTool {

    private final NotifyHub notifyHub;

    public ListContactsTool(NotifyHub notifyHub) {
        this.notifyHub = notifyHub;
    }

    public SyncToolSpecification specification(McpJsonMapper jsonMapper) {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "tag": {
                      "type": "string",
                      "description": "Filter contacts by tag (e.g., \\"vip\\")"
                    }
                  }
                }
                """;

        Tool tool = Tool.builder()
                .name("list_contacts")
                .description("List all contacts in the contact repository, optionally filtered by tag.")
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

    private CallToolResult execute(Map<String, Object> args) {
        AudienceManager audienceManager = notifyHub.getAudienceManager();
        if (audienceManager == null) {
            return ToolResultHelper.error("Audience management is not enabled. Set notify.audiences.enabled=true.");
        }

        ContactRepository repo = audienceManager.getContactRepository();
        String tagFilter = (String) args.get("tag");

        List<Contact> contacts;
        if (tagFilter != null && !tagFilter.isBlank()) {
            contacts = repo.findByTag(tagFilter);
        } else {
            contacts = repo.findAll();
        }

        List<Map<String, Object>> results = new ArrayList<>();
        for (Contact c : contacts) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", c.getId());
            entry.put("name", c.getName());
            if (c.getEmail() != null) entry.put("email", c.getEmail());
            if (c.getPhone() != null) entry.put("phone", c.getPhone());
            if (!c.getTags().isEmpty()) entry.put("tags", c.getTags());
            if (!c.getMetadata().isEmpty()) entry.put("metadata", c.getMetadata());
            results.add(entry);
        }

        return ToolResultHelper.successJson(Map.of(
                "total", results.size(),
                "contacts", results
        ));
    }
}
