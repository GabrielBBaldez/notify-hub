package io.notifyhub.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.notifyhub.core.*;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.dlq.*;
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
class ListDeadLettersToolTest {

    private final McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());

    @Mock
    private NotificationChannel emailChannel;

    private InMemoryDeadLetterQueue dlq;
    private NotifyHub notifyHub;
    private ListDeadLettersTool tool;

    @BeforeEach
    void setUp() {
        when(emailChannel.getName()).thenReturn("email");
        when(emailChannel.sendWithResult(any())).thenCallRealMethod();

        dlq = new InMemoryDeadLetterQueue();
        notifyHub = NotifyHub.builder()
                .channel(emailChannel)
                .tracker(new InMemoryNotificationTracker())
                .deadLetterQueue(dlq)
                .build();

        tool = new ListDeadLettersTool(notifyHub);
    }

    @Test
    @DisplayName("specification() creates tool with correct name and schema")
    void specificationHasCorrectName() {
        SyncToolSpecification spec = tool.specification(jsonMapper);
        assertEquals("list_dead_letters", spec.tool().name());
        assertNotNull(spec.tool().description());
        assertNotNull(spec.tool().inputSchema());
    }

    @Test
    @DisplayName("Returns empty list when DLQ has no entries")
    void emptyDlq() {
        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of());

        assertFalse(result.isError());
        String content = result.content().get(0).toString();
        assertTrue(content.contains("0"));
    }

    @Test
    @DisplayName("Lists dead letters after enqueuing one")
    void listAfterEnqueuing() {
        dlq.enqueue(DeadLetter.builder()
                .channelName("email")
                .recipient("user@test.com")
                .subject("Subject")
                .errorMessage("Error msg")
                .attemptCount(3)
                .templateName("template-name")
                .build());

        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of());

        assertFalse(result.isError());
        String content = result.content().get(0).toString();
        assertTrue(content.contains("user@test.com"));
    }

    @Test
    @DisplayName("Returns error when DLQ is not enabled")
    void errorWhenDlqNotEnabled() {
        NotifyHub hubNoDlq = NotifyHub.builder()
                .channel(emailChannel)
                .tracker(new InMemoryNotificationTracker())
                .build();
        ListDeadLettersTool noDlqTool = new ListDeadLettersTool(hubNoDlq);

        CallToolResult result = noDlqTool.specification(jsonMapper)
                .call()
                .apply(null, Map.of());

        assertTrue(result.isError());
    }
}
