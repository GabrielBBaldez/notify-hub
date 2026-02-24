package io.notifyhub.core.ratelimit;

/**
 * Thrown when a notification cannot be sent because the rate limit
 * for the target channel has been exceeded.
 */
public class RateLimitExceededException extends RuntimeException {

    private final String channelName;

    public RateLimitExceededException(String channelName) {
        super("Rate limit exceeded for channel '" + channelName + "'");
        this.channelName = channelName;
    }

    /** The channel that exceeded its rate limit. */
    public String getChannelName() {
        return channelName;
    }
}
