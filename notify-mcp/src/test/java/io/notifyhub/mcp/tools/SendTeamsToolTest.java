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
class SendTeamsToolTest {

    @Mock
    private NotificationChannel teamsChannel;

    private NotifyHub notifyHub;
    private SendTeamsTool tool;

    @BeforeEach
    void setUp() {
        when(teamsChannel.getName()).thenReturn("teams");

        notifyHub = NotifyHub.builder()
                .channel(teamsChannel)
                .tracker(new InMemoryNotificationTracker())
                .build();

        tool = new SendTeamsTool(notifyHub);
    }

    @Test
    @DisplayName("specification() creates tool with correct name and schema")
    void specificationHasCorrectName() {
        SyncToolSpecification spec = tool.specification(jsonMapper);
        assertEquals("send_teams", spec.tool().name());
        assertNotNull(spec.tool().description());
        assertNotNull(spec.tool().inputSchema());
    }

    @Test
    @DisplayName("Sends Teams message successfully with body")
    void sendTeamsWithBody() {
        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "recipient", "my-team-channel",
                        "body", "Welcome!"
                ));

        assertFalse(result.isError());
        assertNotNull(result.content());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(teamsChannel).send(captor.capture());
        assertEquals("my-team-channel", captor.getValue().getRecipient());
    }

    @Test
    @DisplayName("Returns error when neither body nor template is provided")
    void errorWhenNoBodyOrTemplate() {
        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of("recipient", "my-team-channel"));

        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Returns error when channel send fails")
    void errorOnSendFailure() {
        doThrow(new NotificationSendException("teams", "Connection refused"))
                .when(teamsChannel).send(any());

        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "recipient", "my-team-channel",
                        "body", "Test"
                ));

        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Sends Teams message with template and params")
    void sendTeamsWithTemplate() {
        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "recipient", "my-team-channel",
                        "template", "order-confirmed",
                        "params", Map.of("orderId", "ORD-123")
                ));

        assertFalse(result.isError());
        verify(teamsChannel).send(any());
    }
}
