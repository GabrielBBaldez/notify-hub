package io.notifyhub.spring.actuator;

import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.resilience.ChannelCircuitBreaker;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring Boot Actuator {@link HealthIndicator} for NotifyHub channels.
 *
 * <p>Reports health based on channel availability:</p>
 * <ul>
 *   <li>{@code UP} — all channels are available</li>
 *   <li>{@code DEGRADED} — some channels are down, but at least one is up</li>
 *   <li>{@code DOWN} — all channels are unavailable</li>
 * </ul>
 *
 * <p>When a {@link ChannelCircuitBreaker} is provided, each channel detail also
 * includes the current circuit state (CLOSED, OPEN, or HALF_OPEN).</p>
 *
 * <p>Accessible at {@code /actuator/health/notifyhub}.</p>
 */
public class NotifyHubHealthIndicator implements HealthIndicator {

    private final List<NotificationChannel> channels;
    private final ChannelCircuitBreaker circuitBreaker;

    public NotifyHubHealthIndicator(List<NotificationChannel> channels) {
        this(channels, null);
    }

    public NotifyHubHealthIndicator(List<NotificationChannel> channels, ChannelCircuitBreaker circuitBreaker) {
        this.channels = channels;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public Health health() {
        if (channels == null || channels.isEmpty()) {
            return Health.unknown()
                    .withDetail("reason", "No notification channels registered")
                    .build();
        }

        Health.Builder builder = Health.up();
        int availableCount = 0;
        int totalCount = channels.size();

        for (NotificationChannel channel : channels) {
            boolean available;
            try {
                available = channel.isAvailable();
            } catch (Exception e) {
                available = false;
            }

            if (circuitBreaker != null) {
                Map<String, String> channelDetails = new LinkedHashMap<>();
                channelDetails.put("status", available ? "UP" : "DOWN");
                channelDetails.put("circuitBreaker", circuitBreaker.getState(channel.getName()).name());
                builder.withDetail(channel.getName(), channelDetails);
            } else {
                builder.withDetail(channel.getName(), available ? "UP" : "DOWN");
            }

            if (available) {
                availableCount++;
            }
        }

        builder.withDetail("totalChannels", totalCount);
        builder.withDetail("availableChannels", availableCount);

        if (availableCount == 0) {
            return builder.down()
                    .withDetail("reason", "All notification channels are unavailable")
                    .build();
        } else if (availableCount < totalCount) {
            return builder.status("DEGRADED")
                    .withDetail("reason", (totalCount - availableCount) + " of " + totalCount + " channels unavailable")
                    .build();
        }

        return builder.build();
    }
}
