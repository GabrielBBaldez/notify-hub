package io.notifyhub.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.notifyhub.core.*;
import io.notifyhub.core.channel.NotificationChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class ListScheduledNotificationsToolTest {

    private final McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());

    @Mock
    private NotificationChannel emailChannel;

    @Mock
    private NotificationChannel slackChannel;

    private NotifyHub notifyHub;
    private ScheduledNotificationRegistry registry;
    private ScheduleNotificationTool scheduleTool;
    private ListScheduledNotificationsTool listTool;

    @BeforeEach
    void setUp() {
        when(emailChannel.getName()).thenReturn("email");
        when(emailChannel.sendWithResult(any())).thenCallRealMethod();
        when(slackChannel.getName()).thenReturn("slack");
        when(slackChannel.sendWithResult(any())).thenCallRealMethod();

        notifyHub = NotifyHub.builder()
                .channel(emailChannel)
                .channel(slackChannel)
                .tracker(new InMemoryNotificationTracker())
                .build();

        registry = new ScheduledNotificationRegistry();
        scheduleTool = new ScheduleNotificationTool(notifyHub, registry);
        listTool = new ListScheduledNotificationsTool(registry);
    }

    @AfterEach
    void tearDown() {
        registry.findAll().forEach(sn -> {
            if (!sn.isDone() && !sn.isCancelled()) {
                sn.cancel();
            }
        });
    }

    @Test
    @DisplayName("Should return empty list when no scheduled notifications")
    void emptyList() {
        CallToolResult result = listTool.specification(jsonMapper)
                .call()
                .apply(null, Map.of());

        assertFalse(result.isError());
        String content = result.content().get(0).toString();
        assertTrue(content.contains("\"total\" : 0"));
    }

    @Test
    @DisplayName("Should list scheduled notifications after scheduling")
    void listAfterScheduling() {
        scheduleEmail("user1@test.com");
        scheduleEmail("user2@test.com");

        CallToolResult result = listTool.specification(jsonMapper)
                .call()
                .apply(null, Map.of());

        assertFalse(result.isError());
        String content = result.content().get(0).toString();
        assertTrue(content.contains("\"total\" : 2"));
        assertTrue(content.contains("\"returned\" : 2"));
    }

    @Test
    @DisplayName("Should filter by channel")
    void filterByChannel() {
        scheduleEmail("user@test.com");
        scheduleSlack("#general");

        CallToolResult result = listTool.specification(jsonMapper)
                .call()
                .apply(null, Map.of("channel", "email"));

        assertFalse(result.isError());
        String content = result.content().get(0).toString();
        assertTrue(content.contains("\"returned\" : 1"));
        assertTrue(content.contains("\"channel\" : \"email\""));
    }

    @Test
    @DisplayName("Should respect limit parameter")
    void respectsLimit() {
        scheduleEmail("user1@test.com");
        scheduleEmail("user2@test.com");
        scheduleEmail("user3@test.com");

        CallToolResult result = listTool.specification(jsonMapper)
                .call()
                .apply(null, Map.of("limit", 2));

        assertFalse(result.isError());
        String content = result.content().get(0).toString();
        assertTrue(content.contains("\"total\" : 3"));
        assertTrue(content.contains("\"returned\" : 2"));
    }

    private void scheduleEmail(String recipient) {
        scheduleTool.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "channel", "email",
                        "recipient", recipient,
                        "body", "Test message",
                        "delay", "1h"
                ));
    }

    private void scheduleSlack(String recipient) {
        scheduleTool.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "channel", "slack",
                        "recipient", recipient,
                        "body", "Test message",
                        "delay", "1h"
                ));
    }
}
