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
class CreateContactToolTest {

    private final McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());

    @Mock
    private NotificationChannel emailChannel;

    private NotifyHub notifyHub;
    private CreateContactTool tool;

    @BeforeEach
    void setUp() {
        when(emailChannel.getName()).thenReturn("email");
        when(emailChannel.sendWithResult(any())).thenCallRealMethod();

        ContactRepository contactRepo = new InMemoryContactRepository();
        AudienceManager audienceManager = new AudienceManager(contactRepo);

        notifyHub = NotifyHub.builder()
                .channel(emailChannel)
                .tracker(new InMemoryNotificationTracker())
                .audienceManager(audienceManager)
                .build();

        tool = new CreateContactTool(notifyHub);
    }

    @Test
    @DisplayName("specification() creates tool with correct name")
    void specificationHasCorrectName() {
        SyncToolSpecification spec = tool.specification(jsonMapper);
        assertEquals("create_contact", spec.tool().name());
        assertNotNull(spec.tool().description());
        assertNotNull(spec.tool().inputSchema());
    }

    @Test
    @DisplayName("Creates contact with name successfully")
    void createContactWithName() {
        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of("name", "Alice"));

        assertFalse(result.isError());
        assertNotNull(result.content());
        assertFalse(result.content().isEmpty());
        String text = ((TextContent) result.content().get(0)).text();
        assertTrue(text.contains("Alice"), "Response should contain the contact name 'Alice'");
    }

    @Test
    @DisplayName("Creates contact with tags")
    void createContactWithTags() {
        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "name", "Bob",
                        "email", "bob@test.com",
                        "tags", List.of("vip")
                ));

        assertFalse(result.isError());
        assertNotNull(result.content());
        assertFalse(result.content().isEmpty());
    }

    @Test
    @DisplayName("Returns error when audience management is not enabled")
    void errorWhenAudienceNotEnabled() {
        NotifyHub hubWithoutAudience = NotifyHub.builder()
                .channel(emailChannel)
                .tracker(new InMemoryNotificationTracker())
                .build();

        CreateContactTool toolWithoutAudience = new CreateContactTool(hubWithoutAudience);

        CallToolResult result = toolWithoutAudience.specification(jsonMapper)
                .call()
                .apply(null, Map.of("name", "Alice"));

        assertTrue(result.isError());
    }
}
