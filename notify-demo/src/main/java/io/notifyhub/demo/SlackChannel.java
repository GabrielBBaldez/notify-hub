package io.notifyhub.demo;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Example custom channel — simulates Slack integration.
 * Registered as a Spring bean and auto-discovered by NotifyHub.
 *
 * In a real project, this would call the Slack API.
 */
@Component
public class SlackChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(SlackChannel.class);

    private final List<String> messages = Collections.synchronizedList(new ArrayList<>());

    @Override
    public String getName() {
        return "slack";
    }

    @Override
    public void send(Notification notification) {
        String msg = String.format("[Slack → %s] %s",
                notification.getRecipient(),
                notification.getRenderedContent());
        messages.add(msg);
        log.info("Slack message sent: {}", msg);
    }

    public List<String> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public void clear() {
        messages.clear();
    }
}
