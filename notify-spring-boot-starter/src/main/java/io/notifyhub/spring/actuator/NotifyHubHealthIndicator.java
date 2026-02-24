package io.notifyhub.spring.actuator;

import io.notifyhub.core.channel.NotificationChannel;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.util.List;

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
 * <p>Accessible at {@code /actuator/health/notifyhub}.</p>
 */
public class NotifyHubHealthIndicator implements HealthIndicator {

    private final List<NotificationChannel> channels;

    public NotifyHubHealthIndicator(List<NotificationChannel> channels) {
        this.channels = channels;
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

            builder.withDetail(channel.getName(), available ? "UP" : "DOWN");
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
