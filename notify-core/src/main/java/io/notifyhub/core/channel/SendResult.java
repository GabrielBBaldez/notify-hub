package io.notifyhub.core.channel;

/**
 * Optional result metadata from a channel send operation.
 *
 * <p>Channels that support provider-side tracking (e.g., SendGrid, Mailgun)
 * return a {@code SendResult} containing the provider's message ID.
 * This ID can later be used to query delivery status (opened, bounced, etc.).</p>
 *
 * <p>Channels that don't support tracking (e.g., SMTP) return {@code null}
 * from {@link NotificationChannel#sendWithResult(io.notifyhub.core.Notification)}.</p>
 *
 * @see NotificationChannel#sendWithResult(io.notifyhub.core.Notification)
 * @see EmailStatusProvider
 */
public class SendResult {

    private final String providerMessageId;

    public SendResult(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    /**
     * The provider-assigned message ID for tracking delivery events.
     * For example, SendGrid returns this in the {@code X-Message-Id} response header.
     *
     * @return the provider message ID, or {@code null} if not available
     */
    public String getProviderMessageId() {
        return providerMessageId;
    }

    @Override
    public String toString() {
        return "SendResult{providerMessageId='" + providerMessageId + "'}";
    }
}
