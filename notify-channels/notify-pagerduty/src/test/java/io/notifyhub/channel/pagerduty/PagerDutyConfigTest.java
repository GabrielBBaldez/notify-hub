package io.notifyhub.channel.pagerduty;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PagerDutyConfigTest {

    @Test
    @DisplayName("Should build config with required routing key")
    void testBuilderWithRoutingKey() {
        PagerDutyConfig config = PagerDutyConfig.builder()
                .routingKey("test-routing-key")
                .build();

        assertEquals("test-routing-key", config.getRoutingKey());
    }

    @Test
    @DisplayName("Should default severity to 'warning'")
    void testDefaultSeverity() {
        PagerDutyConfig config = PagerDutyConfig.builder()
                .routingKey("test-routing-key")
                .build();

        assertEquals("warning", config.getSeverity());
    }

    @Test
    @DisplayName("Should accept valid severities")
    void testValidSeverities() {
        for (String severity : new String[]{"critical", "error", "warning", "info"}) {
            PagerDutyConfig config = PagerDutyConfig.builder()
                    .routingKey("test-key")
                    .severity(severity)
                    .build();
            assertEquals(severity, config.getSeverity());
        }
    }

    @Test
    @DisplayName("Should normalize severity to lowercase")
    void testSeverityNormalization() {
        PagerDutyConfig config = PagerDutyConfig.builder()
                .routingKey("test-key")
                .severity("CRITICAL")
                .build();

        assertEquals("critical", config.getSeverity());
    }

    @Test
    @DisplayName("Should throw when routing key is null")
    void testRequiresRoutingKey() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                PagerDutyConfig.builder().build());

        assertTrue(ex.getMessage().contains("routing key"));
    }

    @Test
    @DisplayName("Should throw when routing key is blank")
    void testRejectsBlankRoutingKey() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                PagerDutyConfig.builder().routingKey("   ").build());

        assertTrue(ex.getMessage().contains("routing key"));
    }

    @Test
    @DisplayName("Should throw when severity is invalid")
    void testRejectsInvalidSeverity() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                PagerDutyConfig.builder()
                        .routingKey("test-key")
                        .severity("high")
                        .build());

        assertTrue(ex.getMessage().contains("severity") || ex.getMessage().contains("high"));
    }

    @Test
    @DisplayName("Should use default timeout of 10000ms")
    void testDefaultTimeout() {
        PagerDutyConfig config = PagerDutyConfig.builder()
                .routingKey("test-key")
                .build();

        assertEquals(10_000, config.getTimeoutMs());
    }

    @Test
    @DisplayName("Should allow custom timeout")
    void testCustomTimeout() {
        PagerDutyConfig config = PagerDutyConfig.builder()
                .routingKey("test-key")
                .timeoutMs(5_000)
                .build();

        assertEquals(5_000, config.getTimeoutMs());
    }
}
