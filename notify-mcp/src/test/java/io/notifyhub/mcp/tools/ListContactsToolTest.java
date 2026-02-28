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
class ListContactsToolTest {

    private final McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());

    @Mock
    private NotificationChannel emailChannel;

    private NotifyHub notifyHub;
    private ListContactsTool tool;
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

        tool = new ListContactsTool(notifyHub);
    }

    @Test
    @DisplayName("specification() creates tool with correct name")
    void specificationHasCorrectName() {
        SyncToolSpecification spec = tool.specification(jsonMapper);
        assertEquals("list_contacts", spec.tool().name());
        assertNotNull(spec.tool().description());
        assertNotNull(spec.tool().inputSchema());
    }

    @Test
    @DisplayName("Returns empty list when no contacts exist")
    void emptyList() {
        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of());

        assertFalse(result.isError());
        assertNotNull(result.content());
        assertFalse(result.content().isEmpty());
        String text = ((TextContent) result.content().get(0)).text();
        assertTrue(text.contains("0"), "Response should indicate 0 contacts");
    }

    @Test
    @DisplayName("Lists contacts after creating one")
    void listAfterCreating() {
        Contact contact = Contact.builder()
                .name("Alice")
                .email("alice@test.com")
                .build();
        contactRepo.save(contact);

        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of());

        assertFalse(result.isError());
        String text = ((TextContent) result.content().get(0)).text();
        assertTrue(text.contains("Alice"), "Response should contain contact name 'Alice'");
    }

    @Test
    @DisplayName("Filters contacts by tag")
    void filterByTag() {
        Contact vipContact = Contact.builder()
                .name("Alice")
                .email("alice@test.com")
                .tag("vip")
                .build();
        contactRepo.save(vipContact);

        Contact regularContact = Contact.builder()
                .name("Bob")
                .email("bob@test.com")
                .tag("regular")
                .build();
        contactRepo.save(regularContact);

        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of("tag", "vip"));

        assertFalse(result.isError());
        String text = ((TextContent) result.content().get(0)).text();
        assertTrue(text.contains("Alice"), "Response should contain VIP contact 'Alice'");
        assertFalse(text.contains("Bob"), "Response should not contain non-VIP contact 'Bob'");
    }

    @Test
    @DisplayName("Returns error when audience management is not enabled")
    void errorWhenAudienceNotEnabled() {
        NotifyHub hubWithoutAudience = NotifyHub.builder()
                .channel(emailChannel)
                .tracker(new InMemoryNotificationTracker())
                .build();

        ListContactsTool toolWithoutAudience = new ListContactsTool(hubWithoutAudience);

        CallToolResult result = toolWithoutAudience.specification(jsonMapper)
                .call()
                .apply(null, Map.of());

        assertTrue(result.isError());
    }
}
