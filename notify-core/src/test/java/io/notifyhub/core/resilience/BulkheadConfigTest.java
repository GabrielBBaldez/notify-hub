package io.notifyhub.core.resilience;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BulkheadConfigTest {

    @Test
    @DisplayName("defaults() creates config with maxConcurrentCalls=10")
    void defaultsCreatesConfigWithTenConcurrentCalls() {
        BulkheadConfig config = BulkheadConfig.defaults();
        assertEquals(10, config.getMaxConcurrentCalls());
    }

    @Test
    @DisplayName("perChannel(N) creates config with the given value")
    void perChannelCreatesConfigWithGivenValue() {
        BulkheadConfig config = BulkheadConfig.perChannel(25);
        assertEquals(25, config.getMaxConcurrentCalls());
    }

    @Test
    @DisplayName("Invalid maxConcurrentCalls (0) throws IllegalArgumentException")
    void zeroMaxConcurrentCallsThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> BulkheadConfig.perChannel(0));
    }

    @Test
    @DisplayName("Invalid maxConcurrentCalls (negative) throws IllegalArgumentException")
    void negativeMaxConcurrentCallsThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> BulkheadConfig.perChannel(-5));
    }
}
