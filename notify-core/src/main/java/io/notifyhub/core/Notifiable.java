package io.notifyhub.core;

/**
 * Interface that any entity (User, Customer, etc.) can implement
 * to be used as a notification recipient.
 *
 * <pre>{@code
 * public class User implements Notifiable {
 *     public String getNotifyEmail() { return email; }
 *     public String getNotifyPhone() { return phone; }
 *     public String getNotifyPushToken() { return pushToken; }
 * }
 * }</pre>
 */
public interface Notifiable {

    /**
     * Email address for email notifications.
     * Return null if not available.
     */
    default String getNotifyEmail() {
        return null;
    }

    /**
     * Phone number for SMS/WhatsApp notifications (E.164 format recommended).
     * Example: "+5548999999999"
     * Return null if not available.
     */
    default String getNotifyPhone() {
        return null;
    }

    /**
     * Push notification token (Firebase, OneSignal, etc.).
     * Return null if not available.
     */
    default String getNotifyPushToken() {
        return null;
    }

    /**
     * Display name for template personalization.
     * Return null if not available.
     */
    default String getNotifyName() {
        return null;
    }

    /**
     * Preferred notification channels, in order of priority.
     * The first channel is the primary, the rest are fallbacks.
     *
     * <p>Return an empty list to use the channels specified via
     * {@code .via()} in the builder instead.</p>
     *
     * <pre>{@code
     * public List<Channel> getPreferredChannels() {
     *     return List.of(Channel.TELEGRAM, Channel.EMAIL);
     * }
     * }</pre>
     */
    default java.util.List<Channel> getPreferredChannels() {
        return java.util.List.of();
    }

    /**
     * User's preferred locale for i18n template resolution.
     * Return null to use the system default locale.
     */
    default java.util.Locale getLocale() {
        return null;
    }
}
