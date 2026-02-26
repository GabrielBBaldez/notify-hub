package io.notifyhub.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.notifyhub.core.NotifyHub;
import io.notifyhub.mcp.tools.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class McpServerRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(McpServerRunner.class);

    private final NotifyHub notifyHub;

    public McpServerRunner(NotifyHub notifyHub) {
        this.notifyHub = notifyHub;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting NotifyHub MCP Server...");
        log.info("Registered channels: {}", notifyHub.getRegisteredChannels());

        McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());

        StdioServerTransportProvider transport = new StdioServerTransportProvider(jsonMapper);

        McpSyncServer server = McpServer.sync(transport)
                .serverInfo("notify-hub", "0.5.1")
                .capabilities(ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .build();

        // Register all notification tools
        server.addTool(new SendNotificationTool(notifyHub).specification(jsonMapper));
        server.addTool(new SendEmailTool(notifyHub).specification(jsonMapper));
        server.addTool(new SendSmsTool(notifyHub).specification(jsonMapper));
        server.addTool(new SendSlackTool(notifyHub).specification(jsonMapper));
        server.addTool(new SendTelegramTool(notifyHub).specification(jsonMapper));
        server.addTool(new SendDiscordTool(notifyHub).specification(jsonMapper));
        server.addTool(new SendWhatsAppTool(notifyHub).specification(jsonMapper));
        server.addTool(new SendTeamsTool(notifyHub).specification(jsonMapper));
        server.addTool(new SendGoogleChatTool(notifyHub).specification(jsonMapper));
        server.addTool(new SendPushTool(notifyHub).specification(jsonMapper));
        server.addTool(new SendMultiChannelTool(notifyHub).specification(jsonMapper));
        server.addTool(new ListChannelsTool(notifyHub).specification(jsonMapper));
        server.addTool(new ListDeliveryReceiptsTool(notifyHub).specification(jsonMapper));

        log.info("NotifyHub MCP Server ready — 13 tools registered");
    }
}
