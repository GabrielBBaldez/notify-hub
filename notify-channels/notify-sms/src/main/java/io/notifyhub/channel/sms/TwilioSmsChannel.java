package io.notifyhub.channel.sms;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SMS notification channel using Twilio API.
 *
 * <pre>{@code
 * TwilioConfig config = TwilioConfig.builder()
 *     .accountSid("ACXXXXXXXXXX")
 *     .authToken("your-token")
 *     .fromNumber("+5548999999999")
 *     .build();
 *
 * TwilioSmsChannel smsChannel = new TwilioSmsChannel(config);
 * }</pre>
 */
public class TwilioSmsChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsChannel.class);

    private final TwilioConfig config;
    private boolean initialized = false;

    public TwilioSmsChannel(TwilioConfig config) {
        this.config = config;
    }

    @Override
    public String getName() {
        return "sms";
    }

    @Override
    public void send(Notification notification) {
        ensureInitialized();

        try {
            String content = notification.getRenderedContent();

            // SMS has 1600 char limit per segment, truncate if needed
            if (content.length() > 1600) {
                log.warn("SMS content truncated from {} to 1600 chars", content.length());
                content = content.substring(0, 1597) + "...";
            }

            Message message = Message.creator(
                    new PhoneNumber(notification.getRecipient()),
                    new PhoneNumber(config.getFromNumber()),
                    content
            ).create();

            log.debug("SMS sent to '{}', SID: {}", notification.getRecipient(), message.getSid());

        } catch (Exception e) {
            throw new NotificationSendException("sms",
                    "Failed to send SMS to '" + notification.getRecipient() + "': " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            ensureInitialized();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private synchronized void ensureInitialized() {
        if (!initialized) {
            Twilio.init(config.getAccountSid(), config.getAuthToken());
            initialized = true;
            log.info("Twilio SMS channel initialized");
        }
    }
}
