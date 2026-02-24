package io.notifyhub.spring.actuator;

import io.notifyhub.core.NotificationTracker;
import io.notifyhub.core.channel.NotificationChannel;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring Boot Actuator {@link InfoContributor} for NotifyHub.
 *
 * <p>Adds the following information to {@code /actuator/info}:</p>
 * <ul>
 *   <li>{@code version} — the library version</li>
 *   <li>{@code channels} — list of registered channel names</li>
 *   <li>{@code tracking.enabled} — whether delivery tracking is active</li>
 * </ul>
 */
public class NotifyHubInfoContributor implements InfoContributor {

    private final List<NotificationChannel> channels;
    private final NotificationTracker tracker;
    private final boolean dlqEnabled;

    public NotifyHubInfoContributor(List<NotificationChannel> channels,
                                     NotificationTracker tracker,
                                     boolean dlqEnabled) {
        this.channels = channels;
        this.tracker = tracker;
        this.dlqEnabled = dlqEnabled;
    }

    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("version", "0.3.0");
        info.put("channels", channels.stream()
                .map(NotificationChannel::getName)
                .toList());
        info.put("tracking.enabled", tracker != null);
        info.put("dlq.enabled", dlqEnabled);

        builder.withDetail("notifyhub", info);
    }
}
