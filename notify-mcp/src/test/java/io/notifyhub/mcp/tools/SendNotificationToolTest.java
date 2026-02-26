package io.notifyhub.mcp.tools;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.notifyhub.core.*;
import io.notifyhub.core.channel.NotificationChannel;
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
class SendNotificationToolTest {

    @Mock
    private NotificationChannel emailChannel;

    @Mock
    private NotificationChannel smsChannel;

    @Mock
    private NotificationChannel slackChannel;

    private NotifyHub notifyHub;
    private SendNotificationTool tool;

    @BeforeEach
    void setUp() {
        when(emailChannel.getName()).thenReturn("email");
        when(smsChannel.getName()).thenReturn("sms");
        when(slackChannel.getName()).thenReturn("slack");

        notifyHub = NotifyHub.builder()
                .channel(emailChannel)
                .channel(smsChannel)
                .channel(slackChannel)
                .tracker(new InMemoryNotificationTracker())
                .build();

        tool = new SendNotificationTool(notifyHub);
    }

    @Test
    @DisplayName("Sends notification via email channel")
    void sendViaEmail() {
        CallToolResult result = tool.specification()
                .call()
                .apply(null, Map.of(
                        "channel", "email",
                        "recipient", "user@test.com",
                        "body", "Hello!"
                ));

        assertFalse(result.isError());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(emailChannel).send(captor.capture());
        assertEquals("user@test.com", captor.getValue().getRecipient());
    }

    @Test
    @DisplayName("Sends notification via SMS using toPhone()")
    void sendViaSms() {
        CallToolResult result = tool.specification()
                .call()
                .apply(null, Map.of(
                        "channel", "sms",
                        "recipient", "+5548999999999",
                        "body", "Your code: 1234"
                ));

        assertFalse(result.isError());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(smsChannel).send(captor.capture());
        assertEquals("+5548999999999", captor.getValue().getRecipient());
    }

    @Test
    @DisplayName("Sends notification via Slack")
    void sendViaSlack() {
        CallToolResult result = tool.specification()
                .call()
                .apply(null, Map.of(
                        "channel", "slack",
                        "recipient", "#general",
                        "body", "Deploy complete!"
                ));

        assertFalse(result.isError());
        verify(slackChannel).send(any());
    }

    @Test
    @DisplayName("Returns error when body and template are both missing")
    void errorWhenNoContent() {
        CallToolResult result = tool.specification()
                .call()
                .apply(null, Map.of(
                        "channel", "email",
                        "recipient", "user@test.com"
                ));

        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Sends with priority URGENT")
    void sendWithPriority() {
        CallToolResult result = tool.specification()
                .call()
                .apply(null, Map.of(
                        "channel", "email",
                        "recipient", "user@test.com",
                        "body", "URGENT!",
                        "priority", "URGENT"
                ));

        assertFalse(result.isError());
        verify(emailChannel).send(any());
    }
}
