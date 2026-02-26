package io.notifyhub.mcp.tools;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.notifyhub.channel.discord.DiscordChannel;
import io.notifyhub.channel.discord.DiscordConfig;
import io.notifyhub.core.InMemoryNotificationTracker;
import io.notifyhub.core.NotifyHub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that sends a REAL Discord message.
 * Only runs when DISCORD_WEBHOOK_URL environment variable is set.
 *
 * Run with: mvn test -pl notify-mcp -Dtest=SendDiscordToolIntegrationTest -DDISCORD_WEBHOOK_URL=https://...
 */
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "DISCORD_WEBHOOK_URL", matches = ".+")
class SendDiscordToolIntegrationTest {

    private SendDiscordTool tool;

    @BeforeEach
    void setUp() {
        String webhookUrl = System.getenv("DISCORD_WEBHOOK_URL");

        DiscordChannel discordChannel = new DiscordChannel(
                DiscordConfig.builder()
                        .webhookUrl(webhookUrl)
                        .username("NotifyHub MCP")
                        .build()
        );

        NotifyHub notifyHub = NotifyHub.builder()
                .channel(discordChannel)
                .tracker(new InMemoryNotificationTracker())
                .build();

        tool = new SendDiscordTool(notifyHub);
    }

    @Test
    @DisplayName("Sends a real Discord message via MCP tool")
    void sendRealDiscordMessage() {
        CallToolResult result = tool.specification()
                .call()
                .apply(null, Map.of(
                        "recipient", "mcp-test",
                        "body", "Hello from NotifyHub MCP Server! This message was sent via the send_discord tool."
                ));

        assertFalse(result.isError(), "Expected success but got error: " + result.content());
        String content = result.content().get(0).toString();
        assertTrue(content.contains("SENT"));
    }
}
