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
}
