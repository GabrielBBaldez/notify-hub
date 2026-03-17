package io.notifyhub.core.resilience;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for {@link ChannelCircuitBreaker}.
 *
 * <p>Create via {@link #defaults()} for standard settings or
 * {@link #custom(int, Duration, Duration)} for fine-grained control.</p>
 */
public class CircuitBreakerConfig {

    private static final int DEFAULT_FAILURE_THRESHOLD = 5;
    private static final Duration DEFAULT_OPEN_DURATION = Duration.ofSeconds(30);
    private static final Duration DEFAULT_WINDOW_SIZE = Duration.ofSeconds(60);

    private final int failureThreshold;
    private final Duration openDuration;
    private final Duration windowSize;

    private CircuitBreakerConfig(int failureThreshold, Duration openDuration, Duration windowSize) {
        if (failureThreshold < 1) {
            throw new IllegalArgumentException("failureThreshold must be >= 1");
        }
        Objects.requireNonNull(openDuration, "openDuration must not be null");
        Objects.requireNonNull(windowSize, "windowSize must not be null");
        if (openDuration.isNegative() || openDuration.isZero()) {
            throw new IllegalArgumentException("openDuration must be positive");
        }
        if (windowSize.isNegative() || windowSize.isZero()) {
            throw new IllegalArgumentException("windowSize must be positive");
        }
        this.failureThreshold = failureThreshold;
        this.openDuration = openDuration;
        this.windowSize = windowSize;
    }

    /**
     * Creates a config with sensible defaults:
     * threshold=5, openDuration=30s, windowSize=60s.
     */
    public static CircuitBreakerConfig defaults() {
        return new CircuitBreakerConfig(DEFAULT_FAILURE_THRESHOLD, DEFAULT_OPEN_DURATION, DEFAULT_WINDOW_SIZE);
    }

    /**
     * Creates a fully customised config.
     *
     * @param failureThreshold number of failures within the window before opening the circuit
     * @param openDuration     how long the circuit stays OPEN before transitioning to HALF_OPEN
     * @param windowSize       sliding time window in which failures are counted
     */
    public static CircuitBreakerConfig custom(int failureThreshold, Duration openDuration, Duration windowSize) {
        return new CircuitBreakerConfig(failureThreshold, openDuration, windowSize);
    }

    /** Number of failures within the window that triggers circuit opening. */
    public int getFailureThreshold() {
        return failureThreshold;
    }

    /** How long the circuit remains OPEN before allowing a probe (HALF_OPEN). */
    public Duration getOpenDuration() {
        return openDuration;
    }

    /** Sliding time window for counting failures. Failures older than this are ignored. */
    public Duration getWindowSize() {
        return windowSize;
    }
}
