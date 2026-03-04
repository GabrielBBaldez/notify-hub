package io.notifyhub.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import com.fasterxml.jackson.databind.JsonNode;
import io.notifyhub.core.NotifyHub;
import io.notifyhub.mcp.tools.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
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

        // Check for updates in background (non-blocking, max once per day)
        Thread.ofVirtual().name("update-checker").start(this::checkForUpdatesQuietly);

        // Keep the process alive — MCP STDIO transport needs the JVM running
        Runtime.getRuntime().addShutdownHook(new Thread(keepAlive::countDown));
        keepAlive.await();
    }

    /**
     * Background update check — runs silently, logs only if a new version exists.
     * Checks at most once every 24 hours (caches last check timestamp).
     */
    private void checkForUpdatesQuietly() {
        try {
            // Rate limit: max 1 check per 24h
            Path cacheFile = Path.of(System.getProperty("user.home"), ".notifyhub", "last-update-check");
            if (Files.exists(cacheFile)) {
                Instant lastCheck = Instant.ofEpochMilli(Long.parseLong(Files.readString(cacheFile).trim()));
                if (Duration.between(lastCheck, Instant.now()).toHours() < 24) {
                    return; // Already checked recently
                }
            }

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/GabrielBBaldez/notify-hub/releases/latest"))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "NotifyHub-MCP/" + NotifyMcpServer.VERSION)
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return;

            JsonNode release = new ObjectMapper().readTree(resp.body());
            String latestTag = release.path("tag_name").asText("");
            String latest = latestTag.startsWith("v") ? latestTag.substring(1) : latestTag;

            if (!latest.isBlank() && isNewer(latest, NotifyMcpServer.VERSION)) {
                log.warn("New version available: v{} (current: v{}). Run: notifyhub-mcp --update",
                        latest, NotifyMcpServer.VERSION);
            }

            // Save check timestamp
            Files.createDirectories(cacheFile.getParent());
            Files.writeString(cacheFile, String.valueOf(Instant.now().toEpochMilli()));

        } catch (Exception e) {
            // Silent — never interrupt the MCP server for an update check failure
            log.debug("Update check failed: {}", e.getMessage());
        }
    }

    private static boolean isNewer(String remote, String local) {
        String[] r = remote.split("[.\\-]");
        String[] l = local.split("[.\\-]");
        for (int i = 0; i < Math.max(r.length, l.length); i++) {
            int rv = i < r.length ? parseIntSafe(r[i]) : 0;
            int lv = i < l.length ? parseIntSafe(l[i]) : 0;
            if (rv > lv) return true;
            if (rv < lv) return false;
        }
        return false;
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }
}
