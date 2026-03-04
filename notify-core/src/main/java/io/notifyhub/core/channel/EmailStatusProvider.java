package io.notifyhub.core.channel;

import io.notifyhub.core.DeliveryStatus;

/**
 * Interface for email channels that support delivery event tracking.
 *
 * <p>Implemented by API-based email providers (SendGrid, Mailgun, Amazon SES, etc.)
 * that can query delivery status after sending. SMTP channels do NOT implement this.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * NotificationChannel channel = notifyHub.getChannel("email").orElseThrow();
 * if (channel instanceof EmailStatusProvider provider) {
 *     DeliveryStatus status = provider.checkStatus(receipt.getProviderMessageId());
 *     // status: DELIVERED, OPENED, BOUNCED, SPAM_COMPLAINT, etc.
 * }
 * }</pre>
 *
 * @see SendResult
 * @see io.notifyhub.core.DeliveryStatus
 */
public interface EmailStatusProvider {

    /**
     * Query the provider API for the current delivery status of a previously sent email.
     *
     * @param providerMessageId the message ID returned by the provider at send time
     * @return the current delivery status from the provider
     * @throws NotificationSendException if the status check fails
     */
    DeliveryStatus checkStatus(String providerMessageId);

    /**
     * Whether this channel supports delivery event tracking.
     * Always returns {@code true} for implementations of this interface.
     */
    default boolean supportsTracking() {
        return true;
    }
}
