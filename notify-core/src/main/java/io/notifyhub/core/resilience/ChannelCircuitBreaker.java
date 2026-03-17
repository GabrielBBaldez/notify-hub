package io.notifyhub.core.resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-channel circuit breaker that prevents cascading failures by stopping requests
 * to a channel that is repeatedly failing.
 *
 * <p>State machine per channel:</p>
 * <pre>
 *   CLOSED ──(failures >= threshold)──▶ OPEN
 *   OPEN   ──(openDuration elapsed)───▶ HALF_OPEN
 *   HALF_OPEN ──(probe success)────────▶ CLOSED
 *   HALF_OPEN ──(probe failure)────────▶ OPEN
 * </pre>
 *
 * <p>Thread-safe: all state mutations for a channel are synchronized on the
 * per-channel {@link ChannelBreaker} instance.</p>
 */
public class ChannelCircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(ChannelCircuitBreaker.class);

    private final CircuitBreakerConfig config;
    private final ConcurrentHashMap<String, ChannelBreaker> breakers = new ConcurrentHashMap<>();

    public ChannelCircuitBreaker(CircuitBreakerConfig config) {
        this.config = config;
    }

    /**
     * Determines whether a request for the given channel should be allowed through.
     *
     * <ul>
     *   <li>CLOSED: always allowed.</li>
     *   <li>OPEN: rejected, unless {@code openDuration} has elapsed, in which case
     *       the circuit transitions to HALF_OPEN and the single probe is allowed.</li>
     *   <li>HALF_OPEN: the probe is already in-flight; subsequent requests are rejected.</li>
     * </ul>
     *
     * @param channelName the channel identifier
     * @return {@code true} if the request should proceed, {@code false} if it should be rejected
     */
    public boolean allowRequest(String channelName) {
        ChannelBreaker breaker = breakers.computeIfAbsent(channelName, k -> new ChannelBreaker());
        synchronized (breaker) {
            switch (breaker.state) {
                case CLOSED:
                    return true;

                case OPEN:
                    Instant now = Instant.now();
                    if (breaker.openedAt != null
                            && now.isAfter(breaker.openedAt.plus(config.getOpenDuration()))) {
                        log.debug("Circuit breaker for channel '{}' transitioning OPEN → HALF_OPEN", channelName);
                        breaker.state = CircuitState.HALF_OPEN;
                        return true; // allow the probe
                    }
                    return false;

                case HALF_OPEN:
                    // A probe is already in-flight; reject further requests
                    return false;

                default:
                    return false;
            }
        }
    }

    /**
     * Records a successful send on the given channel.
     *
     * <p>If the channel is in HALF_OPEN state (probe succeeded), transitions to CLOSED
     * and resets failure tracking. No-op when CLOSED.</p>
     *
     * @param channelName the channel identifier
     */
    public void recordSuccess(String channelName) {
        ChannelBreaker breaker = breakers.computeIfAbsent(channelName, k -> new ChannelBreaker());
        synchronized (breaker) {
            if (breaker.state == CircuitState.HALF_OPEN) {
                log.debug("Circuit breaker for channel '{}' transitioning HALF_OPEN → CLOSED (probe succeeded)",
                        channelName);
                breaker.state = CircuitState.CLOSED;
                breaker.failureTimestamps.clear();
                breaker.openedAt = null;
            }
            // CLOSED: no-op — success on a healthy circuit needs no action
        }
    }

    /**
     * Records a failed send on the given channel.
     *
     * <p>In CLOSED state, adds the failure timestamp to the sliding window. If the count
     * of failures within the window reaches the threshold, opens the circuit.</p>
     *
     * <p>In HALF_OPEN state (probe failed), immediately re-opens the circuit.</p>
     *
     * @param channelName the channel identifier
     */
    public void recordFailure(String channelName) {
        ChannelBreaker breaker = breakers.computeIfAbsent(channelName, k -> new ChannelBreaker());
        synchronized (breaker) {
            Instant now = Instant.now();

            if (breaker.state == CircuitState.HALF_OPEN) {
                log.warn("Circuit breaker for channel '{}' probe FAILED — transitioning HALF_OPEN → OPEN",
                        channelName);
                breaker.state = CircuitState.OPEN;
                breaker.openedAt = now;
                return;
            }

            // CLOSED (or OPEN, but OPEN shouldn't normally call recordFailure)
            breaker.failureTimestamps.addLast(now);
            evictExpiredFailures(breaker, now);

            if (breaker.failureTimestamps.size() >= config.getFailureThreshold()) {
                log.warn("Circuit breaker for channel '{}' threshold reached ({} failures) — transitioning CLOSED → OPEN",
                        channelName, breaker.failureTimestamps.size());
                breaker.state = CircuitState.OPEN;
                breaker.openedAt = now;
            }
        }
    }

    /**
     * Returns the current {@link CircuitState} for the given channel.
     * Returns {@link CircuitState#CLOSED} for channels with no tracked state.
     *
     * @param channelName the channel identifier
     * @return current circuit state
     */
    public CircuitState getState(String channelName) {
        ChannelBreaker breaker = breakers.get(channelName);
        if (breaker == null) {
            return CircuitState.CLOSED;
        }
        synchronized (breaker) {
            return breaker.state;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void evictExpiredFailures(ChannelBreaker breaker, Instant now) {
        Instant windowStart = now.minus(config.getWindowSize());
        while (!breaker.failureTimestamps.isEmpty()
                && breaker.failureTimestamps.peekFirst().isBefore(windowStart)) {
            breaker.failureTimestamps.pollFirst();
        }
    }

    // ── Inner class ───────────────────────────────────────────────────────────

    /**
     * Mutable state holder for a single channel. All access must be synchronized on {@code this}.
     */
    private static final class ChannelBreaker {
        CircuitState state = CircuitState.CLOSED;
        final Deque<Instant> failureTimestamps = new ArrayDeque<>();
        Instant openedAt = null;
    }
}
