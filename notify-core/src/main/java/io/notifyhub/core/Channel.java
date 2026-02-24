package io.notifyhub.core;

/**
 * Built-in notification channel types.
 * Use {@code Channel.custom("slack")} for custom channels.
 */
public enum Channel {

    EMAIL,
    SMS,
    WHATSAPP,
    PUSH;

    /**
     * Creates a reference to a custom channel by name.
     *
     * <pre>{@code
     * notify.to(user)
     *     .via(Channel.custom("slack"))
     *     .template("deploy-alert")
     *     .send();
     * }</pre>
     */
    public static ChannelRef custom(String name) {
        return new ChannelRef(name);
    }
}
