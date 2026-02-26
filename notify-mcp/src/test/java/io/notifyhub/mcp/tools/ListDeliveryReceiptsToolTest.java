package io.notifyhub.mcp.tools;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.notifyhub.core.*;
import io.notifyhub.core.channel.NotificationChannel;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ListDeliveryReceiptsToolTest {

    @Mock
    private NotificationChannel emailChannel;

    private NotifyHub notifyHub;
    private InMemoryNotificationTracker tracker;
    private ListDeliveryReceiptsTool tool;

    @BeforeEach
    void setUp() {
        when(emailChannel.getName()).thenReturn("email");

        tracker = new InMemoryNotificationTracker();
        notifyHub = NotifyHub.builder()
                .channel(emailChannel)
                .tracker(tracker)
                .build();

        tool = new ListDeliveryReceiptsTool(notifyHub);
    }

    @Test
    @DisplayName("Returns empty list when no receipts")
    void emptyReceipts() {
        CallToolResult result = tool.specification()
                .call()
                .apply(null, Map.of());

        assertFalse(result.isError());
        String content = result.content().get(0).toString();
        assertTrue(content.contains("\"total\" : 0"));
    }

    @Test
    @DisplayName("Returns receipts after sending tracked notification")
    void returnsReceiptsAfterSend() {
        // Send a tracked notification to populate the tracker
        notifyHub.to("user@test.com")
                .via(Channel.EMAIL)
                .content("Hello!")
                .sendTracked();

        CallToolResult result = tool.specification()
                .call()
                .apply(null, Map.of());

        assertFalse(result.isError());
        String content = result.content().get(0).toString();
        assertTrue(content.contains("SENT"));
        assertTrue(content.contains("user@test.com"));
    }

    @Test
    @DisplayName("Filters by status")
    void filterByStatus() {
        notifyHub.to("user@test.com")
                .via(Channel.EMAIL)
                .content("Test")
                .sendTracked();

        CallToolResult result = tool.specification()
                .call()
                .apply(null, Map.of("status", "SENT"));

        assertFalse(result.isError());
        String content = result.content().get(0).toString();
        assertTrue(content.contains("SENT"));
    }

    @Test
    @DisplayName("Returns error when tracker not configured")
    void errorWhenNoTracker() {
        NotifyHub hubNoTracker = NotifyHub.builder()
                .channel(emailChannel)
                .build();
        ListDeliveryReceiptsTool noTrackerTool = new ListDeliveryReceiptsTool(hubNoTracker);

        CallToolResult result = noTrackerTool.specification()
                .call()
                .apply(null, Map.of());

        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Respects limit parameter")
    void respectsLimit() {
        // Send multiple notifications
        for (int i = 0; i < 5; i++) {
            notifyHub.to("user" + i + "@test.com")
                    .via(Channel.EMAIL)
                    .content("Message " + i)
                    .sendTracked();
        }

        CallToolResult result = tool.specification()
                .call()
                .apply(null, Map.of("limit", 2));

        assertFalse(result.isError());
        String content = result.content().get(0).toString();
        assertTrue(content.contains("\"returned\" : 2"));
    }
}
