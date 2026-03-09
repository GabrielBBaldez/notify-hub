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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScheduleNotificationToolTest {

    private final McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());

    @Mock
    private NotificationChannel emailChannel;

    @Mock
    private NotificationChannel slackChannel;

    private NotifyHub notifyHub;
    private ScheduledNotificationRegistry registry;
    private ScheduleNotificationTool tool;

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
        tool = new ScheduleNotificationTool(notifyHub, registry);
    }

    @AfterEach
    void tearDown() {
        // Cancel all pending scheduled notifications to avoid leaked threads
        registry.findAll().forEach(sn -> {
            if (!sn.isDone() && !sn.isCancelled()) {
                sn.cancel();
            }
        });
    }

    @Test
    @DisplayName("Should schedule notification with delay")
    void scheduleWithDelay() {
        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "channel", "email",
                        "recipient", "user@test.com",
                        "body", "Reminder!",
                        "delay", "5m"
                ));

        assertFalse(result.isError());
        assertEquals(1, registry.size());

        ScheduledNotification sn = registry.findAll().get(0);
        assertEquals("email", sn.getChannelName());
        assertEquals("user@test.com", sn.getRecipient());
        assertEquals(DeliveryStatus.SCHEDULED, sn.getStatus());
    }

    @Test
    @DisplayName("Should schedule notification with scheduled_at")
    void scheduleWithScheduledAt() {
        String futureTime = LocalDateTime.now().plusHours(2).toString();

        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "channel", "slack",
                        "recipient", "#general",
                        "body", "Scheduled message",
                        "scheduled_at", futureTime
                ));

        assertFalse(result.isError());
        assertEquals(1, registry.size());
    }

    @Test
    @DisplayName("Should schedule notification with timezone")
    void scheduleWithTimezone() {
        String futureTime = LocalDateTime.now().plusHours(2).toString();

        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "channel", "email",
                        "recipient", "user@test.com",
                        "body", "Timezone test",
                        "scheduled_at", futureTime,
                        "timezone", "America/Sao_Paulo"
                ));

        assertFalse(result.isError());
        assertEquals(1, registry.size());
    }

    @Test
    @DisplayName("Should return error when neither delay nor scheduled_at provided")
    void errorWhenNoScheduleParam() {
        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "channel", "email",
                        "recipient", "user@test.com",
                        "body", "Hello!"
                ));

        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return error when both delay and scheduled_at provided")
    void errorWhenBothScheduleParams() {
        Map<String, Object> args = new HashMap<>();
        args.put("channel", "email");
        args.put("recipient", "user@test.com");
        args.put("body", "Hello!");
        args.put("delay", "5m");
        args.put("scheduled_at", LocalDateTime.now().plusHours(1).toString());

        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, args);

        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return error when body and template both missing")
    void errorWhenNoContent() {
        CallToolResult result = tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "channel", "email",
                        "recipient", "user@test.com",
                        "delay", "5m"
                ));

        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should parse delay format: minutes")
    void parseDelayMinutes() {
        assertEquals(Duration.ofMinutes(5), ScheduleNotificationTool.parseDelay("5m"));
    }

    @Test
    @DisplayName("Should parse delay format: hours and minutes")
    void parseDelayHoursMinutes() {
        assertEquals(Duration.ofMinutes(90), ScheduleNotificationTool.parseDelay("1h30m"));
    }

    @Test
    @DisplayName("Should parse delay format: days")
    void parseDelayDays() {
        assertEquals(Duration.ofDays(2), ScheduleNotificationTool.parseDelay("2d"));
    }

    @Test
    @DisplayName("Should parse delay format: seconds")
    void parseDelaySeconds() {
        assertEquals(Duration.ofSeconds(90), ScheduleNotificationTool.parseDelay("90s"));
    }

    @Test
    @DisplayName("Should throw on invalid delay format")
    void parseDelayInvalid() {
        assertThrows(IllegalArgumentException.class, () ->
                ScheduleNotificationTool.parseDelay("invalid"));
    }

    @Test
    @DisplayName("Should store scheduled notification in registry")
    void storedInRegistry() {
        tool.specification(jsonMapper)
                .call()
                .apply(null, Map.of(
                        "channel", "email",
                        "recipient", "user@test.com",
                        "body", "Test",
                        "delay", "10m"
                ));

        assertEquals(1, registry.size());
        ScheduledNotification sn = registry.findAll().get(0);
        assertNotNull(sn.getId());
        assertFalse(sn.isDone());
    }

    @Test
    @DisplayName("Should format duration correctly")
    void formatDuration() {
        assertEquals("0s", ScheduleNotificationTool.formatDuration(Duration.ZERO));
        assertEquals("5m", ScheduleNotificationTool.formatDuration(Duration.ofMinutes(5)));
        assertEquals("1h30m", ScheduleNotificationTool.formatDuration(Duration.ofMinutes(90)));
        assertEquals("2d", ScheduleNotificationTool.formatDuration(Duration.ofDays(2)));
        assertEquals("1m30s", ScheduleNotificationTool.formatDuration(Duration.ofSeconds(90)));
    }
}
