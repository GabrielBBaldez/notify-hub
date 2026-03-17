package io.notifyhub.core.testing;

import io.notifyhub.core.Channel;
import io.notifyhub.core.Notifiable;
import io.notifyhub.core.Notification;
import io.notifyhub.core.NotificationBuilder;
import io.notifyhub.core.NotifyHub;
import io.notifyhub.core.channel.NotificationChannel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A test utility that wraps {@link NotifyHub} with mock channels that capture
 * all sent notifications for use in assertions.
 *
 * <p>Example usage in tests:</p>
 * <pre>{@code
 * TestNotifyHub testHub = TestNotifyHub.create();
 *
 * // Execute code under test
 * myService.sendWelcomeEmail(user, testHub.hub());
 *
 * // Assert what was sent
 * assertEquals(1, testHub.sent().size());
 * assertEquals("email", testHub.sent().get(0).channel());
 * assertEquals("user@example.com", testHub.sent().get(0).recipient());
 * }</pre>
 */
public class TestNotifyHub {

    private final NotifyHub hub;
    private final List<SentNotification> sentNotifications;

    private TestNotifyHub(NotifyHub hub, List<SentNotification> sentNotifications) {
        this.hub = hub;
        this.sentNotifications = sentNotifications;
    }

    /**
     * Create a new {@code TestNotifyHub} with capturing mock channels registered
     * for every built-in {@link Channel} enum value.
     *
     * @return a ready-to-use test hub
     */
    public static TestNotifyHub create() {
        List<SentNotification> captured = Collections.synchronizedList(new ArrayList<>());
        NotifyHub.Builder builder = NotifyHub.builder();

        for (Channel ch : Channel.values()) {
            String name = ch.name().toLowerCase().replace('_', '-');
            builder.channel(new CapturingChannel(name, captured));
        }

        return new TestNotifyHub(builder.build(), captured);
    }

    // ===================== FLUENT API DELEGATION =====================

    /**
     * Start building a notification to an email address.
     *
     * @see NotifyHub#to(String)
     */
    public NotificationBuilder to(String email) {
        return hub.to(email);
    }

    /**
     * Start building a notification to a {@link Notifiable} recipient.
     *
     * @see NotifyHub#to(Notifiable)
     */
    public NotificationBuilder to(Notifiable notifiable) {
        return hub.to(notifiable);
    }

    /**
     * Start building a notification to a phone number.
     *
     * @see NotifyHub#toPhone(String)
     */
    public NotificationBuilder toPhone(String phone) {
        return hub.toPhone(phone);
    }

    /**
     * Start building a notification to a {@link Notifiable} using their preferred channels.
     *
     * @see NotifyHub#notify(Notifiable)
     */
    public NotificationBuilder notify(Notifiable notifiable) {
        return hub.notify(notifiable);
    }

    // ===================== ASSERTION HELPERS =====================

    /**
     * Returns an unmodifiable snapshot of all captured sent notifications.
     *
     * @return list of all sent notifications in the order they were sent
     */
    public List<SentNotification> sent() {
        return Collections.unmodifiableList(new ArrayList<>(sentNotifications));
    }

    /**
     * Returns an unmodifiable list of captured notifications filtered by channel name.
     *
     * @param channelName the channel name to filter by (e.g., {@code "email"}, {@code "slack"})
     * @return notifications sent through the specified channel
     */
    public List<SentNotification> sent(String channelName) {
        return sentNotifications.stream()
                .filter(n -> n.channel().equals(channelName))
                .toList();
    }

    /**
     * Clears all captured notifications. Useful when a single test hub instance
     * is reused across multiple test cases.
     */
    public void reset() {
        sentNotifications.clear();
    }

    /**
     * Returns the underlying {@link NotifyHub} instance.
     * Use this to pass to production code under test.
     *
     * @return the wrapped NotifyHub
     */
    public NotifyHub hub() {
        return hub;
    }

    // ===================== CAPTURING CHANNEL (private) =====================

    private static class CapturingChannel implements NotificationChannel {

        private final String name;
        private final List<SentNotification> captured;

        CapturingChannel(String name, List<SentNotification> captured) {
            this.name = name;
            this.captured = captured;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void send(Notification notification) {
            captured.add(new SentNotification(
                    name,
                    notification.getRecipient(),
                    notification.getSubject(),
                    notification.getRenderedContent(),
                    notification.getTemplateName()
            ));
        }

        @Override
        public boolean isAvailable() {
            return true;
        }
    }
}
