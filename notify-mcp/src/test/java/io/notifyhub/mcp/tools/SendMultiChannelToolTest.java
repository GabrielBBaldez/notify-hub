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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SendMultiChannelToolTest {

    @Mock
    private NotificationChannel emailChannel;

    @Mock
    private NotificationChannel slackChannel;

    private NotifyHub notifyHub;
    private SendMultiChannelTool tool;
    private McpJsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        when(emailChannel.getName()).thenReturn("email");
        when(slackChannel.getName()).thenReturn("slack");

        notifyHub = NotifyHub.builder()
                .channel(emailChannel)
                .channel(slackChannel)
                .tracker(new InMemoryNotificationTracker())
                .build();

        tool = new SendMultiChannelTool(notifyHub);
        jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());
    }

    @Test
    @DisplayName("specification() creates tool with correct name and schema")
    void specificationHasCorrectName() {
        SyncToolSpecification spec = tool.specification(jsonMapper);
        assertEquals("send_multi_channel", spec.tool().name());
        assertNotNull(spec.tool().description());
        assertNotNull(spec.tool().inputSchema());
    }

    @Test
    @DisplayName("Sends notification to multiple channels with body")
    void sendToMultipleChannelsWithBody() {
        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "channels", List.of("email", "slack"),
                        "recipient", "user@test.com",
                        "body", "Hello from multi-channel!"
                ));

        assertFalse(result.isError());
        assertNotNull(result.content());

        verify(emailChannel).send(any());
        verify(slackChannel).send(any());
    }

    @Test
    @DisplayName("Returns error when neither body nor template is provided")
    void errorWhenNoBodyOrTemplate() {
        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "channels", List.of("email", "slack"),
                        "recipient", "user@test.com"
                ));

        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Returns error when channel send fails")
    void errorOnSendFailure() {
        doThrow(new NotificationSendException("email", "SMTP connection refused"))
                .when(emailChannel).send(any());
        doThrow(new NotificationSendException("slack", "Webhook failed"))
                .when(slackChannel).send(any());

        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "channels", List.of("email", "slack"),
                        "recipient", "user@test.com",
                        "body", "Test"
                ));

        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Sends notification with template and params")
    void sendWithTemplateAndParams() {
        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "channels", List.of("email", "slack"),
                        "recipient", "user@test.com",
                        "template", "order-confirmed",
                        "params", Map.of("orderId", "ORD-123")
                ));

        assertFalse(result.isError());
        verify(emailChannel).send(any());
        verify(slackChannel).send(any());
    }
}
