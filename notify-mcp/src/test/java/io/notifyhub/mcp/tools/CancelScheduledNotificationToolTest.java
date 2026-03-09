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
class CancelScheduledNotificationToolTest {

    private final McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());

    @Mock
    private NotificationChannel emailChannel;

    private NotifyHub notifyHub;
    private ScheduledNotificationRegistry registry;
    private ScheduleNotificationTool scheduleTool;
    private CancelScheduledNotificationTool cancelTool;

    @BeforeEach
    void setUp() {
        when(emailChannel.getName()).thenReturn("email");
        when(emailChannel.sendWithResult(any())).thenCallRealMethod();

        notifyHub = NotifyHub.builder()
                .channel(emailChannel)
                .tracker(new InMemoryNotificationTracker())
                .build();

        registry = new ScheduledNotificationRegistry();
        scheduleTool = new ScheduleNotificationTool(notifyHub, registry);
        cancelTool = new CancelScheduledNotificationTool(registry);
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
    @DisplayName("Should cancel a pending scheduled notification")
    void cancelPending() {
        // Schedule a notification
        scheduleTool.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "channel", "email",
                        "recipient", "user@test.com",
                        "body", "Test",
                        "delay", "1h"
                ));

        String id = registry.findAll().get(0).getId();

        // Cancel it
        CallToolResult result = cancelTool.specification(jsonMapper)
                .call()
                .apply(null, Map.of("id", id));

        assertFalse(result.isError());
        String content = result.content().get(0).toString();
        assertTrue(content.contains("cancelled successfully"));

        // Verify it's cancelled
        ScheduledNotification sn = registry.findById(id).get();
        assertTrue(sn.isCancelled());
        assertEquals(DeliveryStatus.CANCELLED, sn.getStatus());
    }

    @Test
    @DisplayName("Should return error for unknown ID")
    void errorForUnknownId() {
        CallToolResult result = cancelTool.specification(jsonMapper)
                .call()
                .apply(null, Map.of("id", "nonexistent-id"));

        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return error when notification already cancelled")
    void errorWhenAlreadyCancelled() {
        // Schedule and cancel
        scheduleTool.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "channel", "email",
                        "recipient", "user@test.com",
                        "body", "Test",
                        "delay", "1h"
                ));

        String id = registry.findAll().get(0).getId();
        registry.findById(id).get().cancel();

        // Try to cancel again
        CallToolResult result = cancelTool.specification(jsonMapper)
                .call()
                .apply(null, Map.of("id", id));

        assertTrue(result.isError());
    }
}
