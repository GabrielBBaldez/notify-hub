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

import java.util.List;
import java.util.concurrent.CountDownLatch;

@Component
public class McpServerRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(McpServerRunner.class);
    private final CountDownLatch keepAlive = new CountDownLatch(1);

    private final NotifyHub notifyHub;

    public McpServerRunner(NotifyHub notifyHub) {
        this.notifyHub = notifyHub;
    }

    @Override
    public void run(String... args) throws Exception {
        // Restore stdout — it was suppressed during Spring Boot startup
        System.setOut(NotifyMcpServer.ORIGINAL_STDOUT);

        log.info("Starting NotifyHub MCP Server...");
        log.info("Registered channels: {}", notifyHub.getRegisteredChannels());

        McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());

        // SDK 0.18.x hardcodes protocolVersions to "2024-11-05" only.
        // Override to also support "2025-11-25" which Claude Code requires.
        StdioServerTransportProvider transport = new StdioServerTransportProvider(jsonMapper) {
            @Override
            public List<String> protocolVersions() {
                return List.of("2024-11-05", "2025-03-26", "2025-06-18", "2025-11-25");
            }
        };

        McpSyncServer server = McpServer.sync(transport)
                .serverInfo("notify-hub", "0.9.0")
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
        server.addTool(new SendTwitterTool(notifyHub).specification(jsonMapper));
        server.addTool(new SendLinkedInTool(notifyHub).specification(jsonMapper));
        server.addTool(new SendNotionTool(notifyHub).specification(jsonMapper));
        server.addTool(new SendTwitchTool(notifyHub).specification(jsonMapper));
        server.addTool(new SendYouTubeTool(notifyHub).specification(jsonMapper));
        server.addTool(new SendInstagramTool(notifyHub).specification(jsonMapper));
        server.addTool(new SendMultiChannelTool(notifyHub).specification(jsonMapper));
        server.addTool(new ListChannelsTool(notifyHub).specification(jsonMapper));
        server.addTool(new ListDeliveryReceiptsTool(notifyHub).specification(jsonMapper));

        // Audience & Contact management tools
        server.addTool(new CreateContactTool(notifyHub).specification(jsonMapper));
        server.addTool(new ListContactsTool(notifyHub).specification(jsonMapper));
        server.addTool(new CreateAudienceTool(notifyHub).specification(jsonMapper));
        server.addTool(new ListAudiencesTool(notifyHub).specification(jsonMapper));
        server.addTool(new SendToAudienceTool(notifyHub).specification(jsonMapper));

        // DLQ, Batch & Analytics tools
        server.addTool(new ListDeadLettersTool(notifyHub).specification(jsonMapper));
        server.addTool(new SendBatchTool(notifyHub).specification(jsonMapper));
        server.addTool(new GetAnalyticsTool(notifyHub).specification(jsonMapper));

        log.info("NotifyHub MCP Server ready — 27 tools registered");

        // Keep the process alive — MCP STDIO transport needs the JVM running
        Runtime.getRuntime().addShutdownHook(new Thread(keepAlive::countDown));
        keepAlive.await();
    }
}
