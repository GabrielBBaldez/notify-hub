package io.notifyhub.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.notifyhub.core.*;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SendGoogleChatToolTest {

    @Mock
    private NotificationChannel googleChatChannel;

    private NotifyHub notifyHub;
    private SendGoogleChatTool tool;
    private McpJsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        when(googleChatChannel.getName()).thenReturn("google-chat");

        notifyHub = NotifyHub.builder()
                .channel(googleChatChannel)
                .tracker(new InMemoryNotificationTracker())
                .build();

        tool = new SendGoogleChatTool(notifyHub);
        jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());
    }

    @Test
    @DisplayName("specification() creates tool with correct name and schema")
    void specificationHasCorrectName() {
        SyncToolSpecification spec = tool.specification(jsonMapper);
        assertEquals("send_google_chat", spec.tool().name());
        assertNotNull(spec.tool().description());
        assertNotNull(spec.tool().inputSchema());
    }

    @Test
    @DisplayName("Sends Google Chat message successfully with body")
    void sendGoogleChatWithBody() {
        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "recipient", "my-space",
                        "body", "Welcome!"
                ));

        assertFalse(result.isError());
        assertNotNull(result.content());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(googleChatChannel).send(captor.capture());
        assertEquals("my-space", captor.getValue().getRecipient());
    }

    @Test
    @DisplayName("Returns error when neither body nor template is provided")
    void errorWhenNoBodyOrTemplate() {
        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of("recipient", "my-space"));

        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Returns error when channel send fails")
    void errorOnSendFailure() {
        doThrow(new NotificationSendException("google-chat", "Connection refused"))
                .when(googleChatChannel).send(any());

        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "recipient", "my-space",
                        "body", "Test"
                ));

        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Sends Google Chat message with template and params")
    void sendGoogleChatWithTemplate() {
        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "recipient", "my-space",
                        "template", "order-confirmed",
                        "params", Map.of("orderId", "ORD-123")
                ));

        assertFalse(result.isError());
        verify(googleChatChannel).send(any());
    }
}
