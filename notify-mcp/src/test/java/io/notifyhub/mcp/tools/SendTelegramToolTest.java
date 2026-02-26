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
class SendTelegramToolTest {

    @Mock
    private NotificationChannel telegramChannel;

    private NotifyHub notifyHub;
    private SendTelegramTool tool;
    private McpJsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        when(telegramChannel.getName()).thenReturn("telegram");

        notifyHub = NotifyHub.builder()
                .channel(telegramChannel)
                .tracker(new InMemoryNotificationTracker())
                .build();

        tool = new SendTelegramTool(notifyHub);
        jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());
    }

    @Test
    @DisplayName("specification() creates tool with correct name and schema")
    void specificationHasCorrectName() {
        SyncToolSpecification spec = tool.specification(jsonMapper);
        assertEquals("send_telegram", spec.tool().name());
        assertNotNull(spec.tool().description());
        assertNotNull(spec.tool().inputSchema());
    }

    @Test
    @DisplayName("Sends Telegram message successfully with body")
    void sendTelegramWithBody() {
        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "recipient", "123456789",
                        "body", "Hello Telegram!"
                ));

        assertFalse(result.isError());
        assertNotNull(result.content());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(telegramChannel).send(captor.capture());
        assertEquals("123456789", captor.getValue().getRecipient());
    }

    @Test
    @DisplayName("Returns error when neither body nor template is provided")
    void errorWhenNoBodyOrTemplate() {
        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of("recipient", "123456789"));

        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Returns error when channel send fails")
    void errorOnSendFailure() {
        doThrow(new NotificationSendException("telegram", "Bot API connection refused"))
                .when(telegramChannel).send(any());

        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "recipient", "123456789",
                        "body", "Test"
                ));

        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Sends Telegram message with template and params")
    void sendTelegramWithTemplate() {
        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "recipient", "123456789",
                        "template", "order-confirmed",
                        "params", Map.of("orderId", "ORD-123")
                ));

        assertFalse(result.isError());
        verify(telegramChannel).send(any());
    }
}
