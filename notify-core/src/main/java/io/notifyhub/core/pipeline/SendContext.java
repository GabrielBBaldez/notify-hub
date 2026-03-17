package io.notifyhub.core.pipeline;

import io.notifyhub.core.Notification;
import io.notifyhub.core.NotificationBuilder;
import io.notifyhub.core.NotificationSpec;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.retry.RetryPolicy;

/**
 * Holds all data needed by pipeline handlers during a single send operation.
 *
 * <p>The {@link NotificationSpec} is a public snapshot of the builder data,
 * created at context construction time (in the same package as
 * {@link NotificationBuilder}). It exposes all fields publicly so pipeline
 * handlers in sub-packages can access them without reflection.</p>
 *
 * <p>The {@code notification} field starts as {@code null} and is populated by
 * {@link TemplateHandler} after building and rendering. Subsequent handlers
 * (e.g., {@link RetrySendHandler}) read the notification from this context.</p>
 */
public class SendContext {

    private final String channelName;
    private final NotificationChannel channel;
    private final NotificationBuilder builder;
    private final NotificationSpec spec;
    private final RetryPolicy retryPolicy;

    /** Mutable — set by TemplateHandler after building and rendering. */
    private Notification notification;

    public SendContext(String channelName, NotificationChannel channel,
                       NotificationBuilder builder, RetryPolicy retryPolicy) {
        this.channelName = channelName;
        this.channel = channel;
        this.builder = builder;
        this.spec = new NotificationSpec(builder, channelName);
        this.retryPolicy = retryPolicy;
    }

    public String getChannelName() {
        return channelName;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    /** The original builder — only use within {@code io.notifyhub.core} package. */
    public NotificationBuilder getBuilder() {
        return builder;
    }

    /**
     * Public snapshot of the builder data. Use this in pipeline handlers
     * instead of accessing {@link #getBuilder()} directly.
     */
    public NotificationSpec getSpec() {
        return spec;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public Notification getNotification() {
        return notification;
    }

    /** Called by {@link TemplateHandler} once the notification has been built and rendered. */
    public void setNotification(Notification notification) {
        this.notification = notification;
    }
}
