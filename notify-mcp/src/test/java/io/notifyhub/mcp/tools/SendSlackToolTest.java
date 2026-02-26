package io.notifyhub.mcp.tools;

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
class SendSlackToolTest {

    @Mock
    private NotificationChannel slackChannel;

    private NotifyHub notifyHub;
    private SendSlackTool tool;

    @BeforeEach
    void setUp() {
        when(slackChannel.getName()).thenReturn("slack");

        notifyHub = NotifyHub.builder()
                .channel(slackChannel)
                .tracker(new InMemoryNotificationTracker())
                .build();

        tool = new SendSlackTool(notifyHub);
    }

    @Test
    @DisplayName("specification() creates tool with correct name and schema")
    void specificationHasCorrectName() {
        SyncToolSpecification spec = tool.specification();
        assertEquals("send_slack", spec.tool().name());
        assertNotNull(spec.tool().description());
        assertNotNull(spec.tool().inputSchema());
    }

    @Test
    @DisplayName("Sends Slack message successfully with body")
    void sendSlackWithBody() {
        CallToolResult result = tool.specification()
                .call()
                .apply(null, Map.of(
                        "recipient", "#general",
                        "body", "Hello Slack!"
                ));

        assertFalse(result.isError());
        assertNotNull(result.content());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(slackChannel).send(captor.capture());
        assertEquals("#general", captor.getValue().getRecipient());
    }

    @Test
    @DisplayName("Returns error when neither body nor template is provided")
    void errorWhenNoBodyOrTemplate() {
        CallToolResult result = tool.specification()
                .call()
                .apply(null, Map.of("recipient", "#general"));

        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Returns error when channel send fails")
    void errorOnSendFailure() {
        doThrow(new NotificationSendException("slack", "Webhook connection refused"))
                .when(slackChannel).send(any());

        CallToolResult result = tool.specification()
                .call()
                .apply(null, Map.of(
                        "recipient", "#general",
                        "body", "Test"
                ));

        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Sends Slack message with template and params")
    void sendSlackWithTemplate() {
        CallToolResult result = tool.specification()
                .call()
                .apply(null, Map.of(
                        "recipient", "#general",
                        "template", "order-confirmed",
                        "params", Map.of("orderId", "ORD-123")
                ));

        assertFalse(result.isError());
        verify(slackChannel).send(any());
    }
}
