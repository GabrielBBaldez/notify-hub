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
class SendPushToolTest {

    @Mock
    private NotificationChannel pushChannel;

    private NotifyHub notifyHub;
    private SendPushTool tool;
    private McpJsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        when(pushChannel.getName()).thenReturn("push");

        notifyHub = NotifyHub.builder()
                .channel(pushChannel)
                .tracker(new InMemoryNotificationTracker())
                .build();

        tool = new SendPushTool(notifyHub);
        jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());
    }

    @Test
    @DisplayName("specification() creates tool with correct name and schema")
    void specificationHasCorrectName() {
        SyncToolSpecification spec = tool.specification(jsonMapper);
        assertEquals("send_push", spec.tool().name());
        assertNotNull(spec.tool().description());
        assertNotNull(spec.tool().inputSchema());
    }

    @Test
    @DisplayName("Sends push notification successfully with body and push_token")
    void sendPushWithBodyAndToken() {
        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "push_token", "fcm-token-123",
                        "body", "Hello from push!"
                ));

        assertFalse(result.isError());
        assertNotNull(result.content());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(pushChannel).send(captor.capture());
        assertEquals("fcm-token-123", captor.getValue().getRecipient());
    }

    @Test
    @DisplayName("Returns error when no body is provided")
    void errorWhenNoBody() {
        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of("push_token", "fcm-token-123"));

        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Sends push notification with title")
    void sendPushWithTitle() {
        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "push_token", "fcm-token-123",
                        "subject", "Alert",
                        "body", "You have a new message"
                ));

        assertFalse(result.isError());
        verify(pushChannel).send(any());
    }

    @Test
    @DisplayName("Returns error when channel send fails")
    void errorOnSendFailure() {
        doThrow(new NotificationSendException("push", "FCM connection refused"))
                .when(pushChannel).send(any());

        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "push_token", "fcm-token-123",
                        "body", "Test"
                ));

        assertTrue(result.isError());
    }
}
