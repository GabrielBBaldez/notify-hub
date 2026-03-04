package io.notifyhub.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.notifyhub.core.*;
import io.notifyhub.core.audience.*;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.dlq.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetAnalyticsToolTest {

    private final McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());

    @Mock
    private NotificationChannel emailChannel;

    private InMemoryNotificationTracker tracker;
    private InMemoryDeadLetterQueue dlq;
    private InMemoryContactRepository contactRepo;
    private AudienceManager audienceManager;
    private NotifyHub notifyHub;
    private GetAnalyticsTool tool;

    @BeforeEach
    void setUp() {
        when(emailChannel.getName()).thenReturn("email");
        when(emailChannel.sendWithResult(any())).thenCallRealMethod();

        tracker = new InMemoryNotificationTracker();
        dlq = new InMemoryDeadLetterQueue();
        contactRepo = new InMemoryContactRepository();
        audienceManager = new AudienceManager(contactRepo);

        notifyHub = NotifyHub.builder()
                .channel(emailChannel)
                .tracker(tracker)
                .deadLetterQueue(dlq)
                .audienceManager(audienceManager)
                .build();

        tool = new GetAnalyticsTool(notifyHub);
    }

    @Test
    @DisplayName("specification() creates tool with correct name and schema")
    void specificationHasCorrectName() {
        SyncToolSpecification spec = tool.specification(jsonMapper);
        assertEquals("get_analytics", spec.tool().name());
        assertNotNull(spec.tool().description());
        assertNotNull(spec.tool().inputSchema());
    }

    @Test
    @DisplayName("Returns analytics with zero totals when no data")
    void analyticsWithNoData() {
        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of());

        assertFalse(result.isError());
        String content = result.content().get(0).toString();
        assertTrue(content.contains("0"));
    }

    @Test
    @DisplayName("Returns analytics indicating tracking is disabled when no tracker")
    void analyticsWithTrackerDisabled() {
        NotifyHub hubNoTracker = NotifyHub.builder()
                .channel(emailChannel)
                .deadLetterQueue(dlq)
                .audienceManager(audienceManager)
                .build();
        GetAnalyticsTool noTrackerTool = new GetAnalyticsTool(hubNoTracker);

        CallToolResult result = noTrackerTool.specification(jsonMapper)
                .call()
                .apply(null, Map.of());

        assertFalse(result.isError());
        String content = result.content().get(0).toString();
        assertTrue(content.contains("disabled"));
    }
}
