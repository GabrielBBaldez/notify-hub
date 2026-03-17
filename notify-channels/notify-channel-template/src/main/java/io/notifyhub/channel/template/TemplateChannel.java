package io.notifyhub.channel.template;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NotifyHub channel implementation for Template.
 *
 * <p>TODO: Replace "Template" with your channel name (e.g., "PagerDuty").</p>
 * <p>TODO: Replace "template" in getName() with your channel's hyphenated name.</p>
 *
 * <h2>Quick Start</h2>
 * <ol>
 *   <li>Copy this module: {@code cp -r notify-channel-template notify-your-channel}</li>
 *   <li>Find-replace "Template" → "YourChannel", "template" → "your-channel"</li>
 *   <li>Update {@link TemplateChannelConfig} with your channel's credentials</li>
 *   <li>Implement {@link #send(Notification)} with your channel's API call</li>
 *   <li>Add the module to the root {@code pom.xml} modules list</li>
 * </ol>
 */
public class TemplateChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(TemplateChannel.class);

    private final TemplateChannelConfig config;

    public TemplateChannel(TemplateChannelConfig config) {
        this.config = config;
    }

    @Override
    public String getName() {
        return "template"; // TODO: Return your channel's hyphenated name (e.g., "pagerduty")
    }

    @Override
    public boolean isAvailable() {
        return config.getApiKey() != null && !config.getApiKey().isBlank();
    }

    @Override
    public void send(Notification notification) throws NotificationSendException {
        log.debug("Sending notification via Template channel to '{}'", notification.getRecipient());

        // TODO: Implement your channel's send logic here.
        // Example:
        //   1. Build the request payload from notification.getRenderedContent()
        //   2. Call your channel's API (HTTP, SDK, etc.)
        //   3. Log success or throw NotificationSendException on failure
        //
        // throw new NotificationSendException(getName(), "Not implemented yet");

        log.info("Template notification sent to '{}'", notification.getRecipient());
    }
}
