package io.notifyhub.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.notifyhub.core.*;
import io.notifyhub.core.audience.*;
import io.notifyhub.core.channel.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ListAudiencesToolTest {

    private final McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());

    @Mock
    private NotificationChannel emailChannel;

    private NotifyHub notifyHub;
    private ListAudiencesTool tool;
    private ContactRepository contactRepo;
    private AudienceManager audienceManager;

    @BeforeEach
    void setUp() {
        when(emailChannel.getName()).thenReturn("email");

        contactRepo = new InMemoryContactRepository();
        audienceManager = new AudienceManager(contactRepo);

        notifyHub = NotifyHub.builder()
                .channel(emailChannel)
                .tracker(new InMemoryNotificationTracker())
                .audienceManager(audienceManager)
                .build();

        tool = new ListAudiencesTool(notifyHub);
    }

    @Test
    @DisplayName("specification() creates tool with correct name")
    void specificationHasCorrectName() {
        SyncToolSpecification spec = tool.specification(jsonMapper);
        assertEquals("list_audiences", spec.tool().name());
        assertNotNull(spec.tool().description());
        assertNotNull(spec.tool().inputSchema());
    }

    @Test
    @DisplayName("Returns empty list when no audiences exist")
    void emptyList() {
        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of());

        assertFalse(result.isError());
        assertNotNull(result.content());
        assertFalse(result.content().isEmpty());
        String text = ((TextContent) result.content().get(0)).text();
        assertTrue(text.contains("0"), "Response should indicate 0 audiences");
    }

    @Test
    @DisplayName("Lists audiences after creating one")
    void listAfterCreating() {
        audienceManager.createAudience("premium-users", Set.of("premium"));

        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of());

        assertFalse(result.isError());
        String text = ((TextContent) result.content().get(0)).text();
        assertTrue(text.contains("premium-users"), "Response should contain audience name 'premium-users'");
    }

    @Test
    @DisplayName("Returns error when audience management is not enabled")
    void errorWhenAudienceNotEnabled() {
        NotifyHub hubWithoutAudience = NotifyHub.builder()
                .channel(emailChannel)
                .tracker(new InMemoryNotificationTracker())
                .build();

        ListAudiencesTool toolWithoutAudience = new ListAudiencesTool(hubWithoutAudience);

        CallToolResult result = toolWithoutAudience.specification(jsonMapper)
                .call()
                .apply(null, Map.of());

        assertTrue(result.isError());
    }
}
