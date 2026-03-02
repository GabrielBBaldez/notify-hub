package io.notifyhub.core;

/**
 * Built-in notification channel types.
 * Use {@code Channel.custom("mychannel")} for custom channels not listed here.
 */
public enum Channel {

    EMAIL,
    SMS,
    WHATSAPP,
    PUSH,
    SLACK,
    TELEGRAM,
    DISCORD,
    TEAMS,
    WEBSOCKET,
    GOOGLE_CHAT,
    TWITTER,
    LINKEDIN,
    NOTION,
    TWITCH,
    YOUTUBE,
    INSTAGRAM;

    /**
     * Creates a reference to a custom channel by name.
     *
     * <pre>{@code
     * notify.to(user)
     *     .via(Channel.custom("my-custom-channel"))
     *     .template("deploy-alert")
     *     .send();
     * }</pre>
     */
    public static ChannelRef custom(String name) {
        return new ChannelRef(name);
    }
}
