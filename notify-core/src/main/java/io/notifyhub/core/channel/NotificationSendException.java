package io.notifyhub.core.channel;

/**
 * Thrown when a notification channel fails to send a notification.
 */
public class NotificationSendException extends RuntimeException {

    private final String channelName;

    public NotificationSendException(String channelName, String message) {
        super("[" + channelName + "] " + message);
        this.channelName = channelName;
    }

    public NotificationSendException(String channelName, String message, Throwable cause) {
        super("[" + channelName + "] " + message, cause);
        this.channelName = channelName;
    }

    public String getChannelName() {
        return channelName;
    }
}
