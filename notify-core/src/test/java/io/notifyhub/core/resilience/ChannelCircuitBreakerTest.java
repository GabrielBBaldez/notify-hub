package io.notifyhub.core.resilience;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ChannelCircuitBreakerTest {

    private ChannelCircuitBreaker circuitBreaker;
    private CircuitBreakerConfig config;

    @BeforeEach
    void setUp() {
        config = CircuitBreakerConfig.custom(3, Duration.ofSeconds(5), Duration.ofSeconds(60));
        circuitBreaker = new ChannelCircuitBreaker(config);
    }

    @Test
    @DisplayName("CLOSED state allows all requests through")
    void testClosedStateAllowsRequests() {
        assertTrue(circuitBreaker.allowRequest("slack"), "CLOSED circuit should allow requests");
        assertEquals(CircuitState.CLOSED, circuitBreaker.getState("slack"));
    }

    @Test
    @DisplayName("Transitions from CLOSED to OPEN after failure threshold is reached within window")
    void testClosedToOpenAfterFailureThreshold() {
        for (int i = 0; i < 3; i++) {
            circuitBreaker.recordFailure("slack");
        }

        assertEquals(CircuitState.OPEN, circuitBreaker.getState("slack"),
                "Circuit should be OPEN after reaching failure threshold");
        assertFalse(circuitBreaker.allowRequest("slack"),
                "OPEN circuit should reject requests");
    }

    @Test
    @DisplayName("OPEN state rejects all requests")
    void testOpenStateRejectsRequests() {
        // Force open by hitting threshold
        for (int i = 0; i < 3; i++) {
            circuitBreaker.recordFailure("email");
        }

        assertFalse(circuitBreaker.allowRequest("email"), "OPEN circuit should reject first request");
        assertFalse(circuitBreaker.allowRequest("email"), "OPEN circuit should reject second request");
    }

    @Test
    @DisplayName("Transitions from OPEN to HALF_OPEN after open duration elapses")
    void testOpenToHalfOpenAfterOpenDuration() throws Exception {
        // Use a very short open duration
        CircuitBreakerConfig shortConfig = CircuitBreakerConfig.custom(
                2, Duration.ofMillis(50), Duration.ofSeconds(60));
        ChannelCircuitBreaker cb = new ChannelCircuitBreaker(shortConfig);

        cb.recordFailure("telegram");
        cb.recordFailure("telegram");
        assertEquals(CircuitState.OPEN, cb.getState("telegram"));

        // Wait for open duration to elapse
        Thread.sleep(100);

        // Next allowRequest should transition to HALF_OPEN and allow one probe
        assertTrue(cb.allowRequest("telegram"),
                "After open duration, circuit should allow one probe (HALF_OPEN)");
        assertEquals(CircuitState.HALF_OPEN, cb.getState("telegram"));
    }

    @Test
    @DisplayName("Transitions from HALF_OPEN to CLOSED on success")
    void testHalfOpenToClosedOnSuccess() throws Exception {
        CircuitBreakerConfig shortConfig = CircuitBreakerConfig.custom(
                2, Duration.ofMillis(50), Duration.ofSeconds(60));
        ChannelCircuitBreaker cb = new ChannelCircuitBreaker(shortConfig);

        cb.recordFailure("discord");
        cb.recordFailure("discord");
        assertEquals(CircuitState.OPEN, cb.getState("discord"));

        Thread.sleep(100);

        // Trigger HALF_OPEN
        assertTrue(cb.allowRequest("discord"));
        assertEquals(CircuitState.HALF_OPEN, cb.getState("discord"));

        // Record success — should close the circuit
        cb.recordSuccess("discord");
        assertEquals(CircuitState.CLOSED, cb.getState("discord"),
                "Successful probe should close the circuit");
        assertTrue(cb.allowRequest("discord"),
                "Closed circuit should allow subsequent requests");
    }

    @Test
    @DisplayName("Transitions from HALF_OPEN to OPEN on failure")
    void testHalfOpenToOpenOnFailure() throws Exception {
        CircuitBreakerConfig shortConfig = CircuitBreakerConfig.custom(
                2, Duration.ofMillis(50), Duration.ofSeconds(60));
        ChannelCircuitBreaker cb = new ChannelCircuitBreaker(shortConfig);

        cb.recordFailure("teams");
        cb.recordFailure("teams");
        assertEquals(CircuitState.OPEN, cb.getState("teams"));

        Thread.sleep(100);

        // Trigger HALF_OPEN
        assertTrue(cb.allowRequest("teams"));
        assertEquals(CircuitState.HALF_OPEN, cb.getState("teams"));

        // Record failure — should re-open the circuit
        cb.recordFailure("teams");
        assertEquals(CircuitState.OPEN, cb.getState("teams"),
                "Failed probe should re-open the circuit");
        assertFalse(cb.allowRequest("teams"),
                "Re-opened circuit should reject requests");
    }

    @Test
    @DisplayName("Old failures outside the sliding window do not count toward threshold")
    void testOldFailuresOutsideWindowDoNotCount() throws Exception {
        CircuitBreakerConfig shortWindowConfig = CircuitBreakerConfig.custom(
                3, Duration.ofSeconds(30), Duration.ofMillis(100));
        ChannelCircuitBreaker cb = new ChannelCircuitBreaker(shortWindowConfig);

        // Record 2 failures (threshold is 3)
        cb.recordFailure("webhook");
        cb.recordFailure("webhook");
        assertEquals(CircuitState.CLOSED, cb.getState("webhook"),
                "Should still be CLOSED — only 2 failures, threshold is 3");

        // Wait for the window to expire
        Thread.sleep(150);

        // Record 2 more failures — but old ones expired, so total in window = 2 < 3
        cb.recordFailure("webhook");
        cb.recordFailure("webhook");
        assertEquals(CircuitState.CLOSED, cb.getState("webhook"),
                "Should still be CLOSED — old failures expired, new window has only 2");
    }

    @Test
    @DisplayName("Circuit breaker state is independent per channel")
    void testIndependentPerChannelState() {
        // Open circuit for "slack"
        for (int i = 0; i < 3; i++) {
            circuitBreaker.recordFailure("slack");
        }

        assertEquals(CircuitState.OPEN, circuitBreaker.getState("slack"),
                "slack circuit should be OPEN");
        assertEquals(CircuitState.CLOSED, circuitBreaker.getState("email"),
                "email circuit should still be CLOSED — independent state");

        assertTrue(circuitBreaker.allowRequest("email"),
                "email channel should still accept requests");
        assertFalse(circuitBreaker.allowRequest("slack"),
                "slack channel should reject requests");
    }
}
