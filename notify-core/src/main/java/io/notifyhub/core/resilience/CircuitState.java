package io.notifyhub.core.resilience;

/**
 * Represents the state of a circuit breaker for a given channel.
 *
 * <ul>
 *   <li>{@link #CLOSED} — Normal operation. Requests flow through.</li>
 *   <li>{@link #OPEN} — Blocked. Requests are rejected immediately.</li>
 *   <li>{@link #HALF_OPEN} — Testing. One probe request is allowed through to check recovery.</li>
 * </ul>
 */
public enum CircuitState {
    CLOSED,
    OPEN,
    HALF_OPEN
}
