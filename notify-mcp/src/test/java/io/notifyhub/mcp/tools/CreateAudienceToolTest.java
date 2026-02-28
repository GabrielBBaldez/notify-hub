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
class CreateAudienceToolTest {

    private final McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());

    @Mock
    private NotificationChannel emailChannel;

    private NotifyHub notifyHub;
    private CreateAudienceTool tool;

    @BeforeEach
    void setUp() {
        when(emailChannel.getName()).thenReturn("email");

        ContactRepository contactRepo = new InMemoryContactRepository();
        AudienceManager audienceManager = new AudienceManager(contactRepo);

        notifyHub = NotifyHub.builder()
                .channel(emailChannel)
                .tracker(new InMemoryNotificationTracker())
                .audienceManager(audienceManager)
                .build();

        tool = new CreateAudienceTool(notifyHub);
    }

    @Test
    @DisplayName("specification() creates tool with correct name")
    void specificationHasCorrectName() {
        SyncToolSpecification spec = tool.specification(jsonMapper);
        assertEquals("create_audience", spec.tool().name());
        assertNotNull(spec.tool().description());
        assertNotNull(spec.tool().inputSchema());
    }

    @Test
    @DisplayName("Creates audience with tags successfully")
    void createAudienceWithTags() {
        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "name", "vips",
                        "tags", List.of("vip")
                ));

        assertFalse(result.isError());
        assertNotNull(result.content());
        assertFalse(result.content().isEmpty());
        String text = ((TextContent) result.content().get(0)).text();
        assertTrue(text.contains("vips"), "Response should contain audience name 'vips'");
    }

    @Test
    @DisplayName("Returns error when no tags provided")
    void errorWhenNoTags() {
        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "name", "test",
                        "tags", List.of()
                ));

        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Returns error when audience management is not enabled")
    void errorWhenAudienceNotEnabled() {
        NotifyHub hubWithoutAudience = NotifyHub.builder()
                .channel(emailChannel)
                .tracker(new InMemoryNotificationTracker())
                .build();

        CreateAudienceTool toolWithoutAudience = new CreateAudienceTool(hubWithoutAudience);

        CallToolResult result = toolWithoutAudience.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "name", "vips",
                        "tags", List.of("vip")
                ));

        assertTrue(result.isError());
    }
}
