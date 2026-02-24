package io.notifyhub.demo;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Demo wrapper for the Slack channel that stores messages in-memory
 * for the /inbox/slack endpoint. In real mode, delegates to the library's
 * SlackChannel; in fake mode, just stores locally.
 */
@Component
public class DemoSlackChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(DemoSlackChannel.class);

    private final List<String> messages = Collections.synchronizedList(new ArrayList<>());

    @Autowired(required = false)
    private io.notifyhub.channel.slack.SlackChannel realSlackChannel;

    @Value("${demo.slack.webhook-url:}")
    private String webhookUrl;

    @Override
    public String getName() {
        return "slack";
    }

    @Override
    public void send(Notification notification) {
        String content = notification.getRenderedContent();
        String recipient = notification.getRecipient();

        // If real Slack channel is configured, delegate to it
        if (realSlackChannel != null) {
            realSlackChannel.send(notification);
        }

        // Always store locally for /inbox/slack
        String msg = String.format("[Slack → %s] %s", recipient, content);
        messages.add(msg);
        log.info("Slack message: {}", msg);
    }

    public List<String> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public void clear() {
        messages.clear();
    }
}
