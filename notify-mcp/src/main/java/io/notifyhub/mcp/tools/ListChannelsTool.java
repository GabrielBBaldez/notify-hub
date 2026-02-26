package io.notifyhub.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.notifyhub.core.NotifyHub;
import io.notifyhub.mcp.util.ToolResultHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ListChannelsTool {

    private final NotifyHub notifyHub;

    public ListChannelsTool(NotifyHub notifyHub) {
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
                .name("list_channels")
                .description("List all configured notification channels and their availability status.")
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
        List<Map<String, Object>> channelInfos = new ArrayList<>();

        for (String name : notifyHub.getRegisteredChannels()) {
            notifyHub.getChannel(name).ifPresent(ch -> {
                boolean available;
                try {
                    available = ch.isAvailable();
                } catch (Exception e) {
                    available = false;
                }
                channelInfos.add(Map.of(
                        "name", name,
                        "available", available
                ));
            });
        }

        return ToolResultHelper.successJson(Map.of(
                "total", channelInfos.size(),
                "channels", channelInfos
        ));
    }
}
