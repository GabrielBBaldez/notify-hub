package io.notifyhub.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.notifyhub.core.NotifyHub;
import io.notifyhub.core.channel.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ListChannelsToolTest {

    private final McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());

    @Mock
    private NotificationChannel emailChannel;

    @Mock
    private NotificationChannel slackChannel;

    @Mock
    private NotificationChannel discordChannel;

    private NotifyHub notifyHub;
    private ListChannelsTool tool;

    @BeforeEach
    void setUp() {
        when(emailChannel.getName()).thenReturn("email");
        when(emailChannel.isAvailable()).thenReturn(true);
        when(emailChannel.sendWithResult(any())).thenCallRealMethod();

        when(slackChannel.getName()).thenReturn("slack");
        when(slackChannel.isAvailable()).thenReturn(true);
        when(slackChannel.sendWithResult(any())).thenCallRealMethod();

        when(discordChannel.getName()).thenReturn("discord");
        when(discordChannel.isAvailable()).thenReturn(false);
        when(discordChannel.sendWithResult(any())).thenCallRealMethod();

        notifyHub = NotifyHub.builder()
                .channel(emailChannel)
                .channel(slackChannel)
                .channel(discordChannel)
                .build();

        tool = new ListChannelsTool(notifyHub);
    }

    @Test
    @DisplayName("specification() creates tool with correct name")
    void specificationHasCorrectName() {
        SyncToolSpecification spec = tool.specification(jsonMapper);
        assertEquals("list_channels", spec.tool().name());
    }

    @Test
    @DisplayName("Lists all registered channels with availability")
    void listsAllChannels() {
        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of());

        assertFalse(result.isError());
        String content = result.content().get(0).toString();
        assertTrue(content.contains("email"));
        assertTrue(content.contains("slack"));
        assertTrue(content.contains("discord"));
    }

    @Test
    @DisplayName("Returns empty list when no channels configured")
    void emptyWhenNoChannels() {
        NotifyHub emptyHub = NotifyHub.builder().build();
        ListChannelsTool emptyTool = new ListChannelsTool(emptyHub);

        CallToolResult result = emptyTool.specification(jsonMapper)
                .call()
                .apply(null, Map.of());

        assertFalse(result.isError());
    }
}
