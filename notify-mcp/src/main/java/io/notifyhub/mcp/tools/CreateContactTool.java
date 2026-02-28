package io.notifyhub.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.notifyhub.core.NotifyHub;
import io.notifyhub.core.audience.AudienceManager;
import io.notifyhub.core.audience.Contact;
import io.notifyhub.mcp.util.ToolResultHelper;

import java.util.*;

public class CreateContactTool {

    private final NotifyHub notifyHub;

    public CreateContactTool(NotifyHub notifyHub) {
        this.notifyHub = notifyHub;
    }

    public SyncToolSpecification specification(McpJsonMapper jsonMapper) {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "name": {
                      "type": "string",
                      "description": "Contact display name"
                    },
                    "email": {
                      "type": "string",
                      "description": "Contact email address"
                    },
                    "phone": {
                      "type": "string",
                      "description": "Contact phone number in E.164 format (e.g., +5548999999999)"
                    },
                    "pushToken": {
                      "type": "string",
                      "description": "Firebase Cloud Messaging push token"
                    },
                    "tags": {
                      "type": "array",
                      "items": { "type": "string" },
                      "description": "Tags for audience segmentation (e.g., [\\"vip\\", \\"plan:premium\\"])"
                    },
                    "metadata": {
                      "type": "object",
                      "description": "Custom key-value metadata for the contact",
                      "additionalProperties": true
                    }
                  },
                  "required": ["name"]
                }
                """;

        Tool tool = Tool.builder()
                .name("create_contact")
                .description("Create a contact for audience segmentation. Contacts can be tagged and grouped into audiences for batch notifications.")
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
        String email = (String) args.get("email");
        String phone = (String) args.get("phone");
        String pushToken = (String) args.get("pushToken");
        List<String> tags = (List<String>) args.get("tags");
        Map<String, Object> metadata = (Map<String, Object>) args.get("metadata");

        Contact.Builder builder = Contact.builder().name(name);
        if (email != null) builder.email(email);
        if (phone != null) builder.phone(phone);
        if (pushToken != null) builder.pushToken(pushToken);
        if (tags != null) builder.tags(new LinkedHashSet<>(tags));
        if (metadata != null) {
            metadata.forEach((k, v) -> builder.metadata(k, String.valueOf(v)));
        }

        Contact contact = builder.build();
        audienceManager.getContactRepository().save(contact);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", contact.getId());
        result.put("name", contact.getName());
        if (contact.getEmail() != null) result.put("email", contact.getEmail());
        if (contact.getPhone() != null) result.put("phone", contact.getPhone());
        if (!contact.getTags().isEmpty()) result.put("tags", contact.getTags());

        return ToolResultHelper.success("Contact '" + name + "' created", result);
    }
}
