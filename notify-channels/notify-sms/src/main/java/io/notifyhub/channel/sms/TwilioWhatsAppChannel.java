package io.notifyhub.channel.sms;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;
import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;

/**
 * WhatsApp notification channel using Twilio API.
 *
 * <p>Uses the same Twilio SDK as SMS — the difference is that
 * phone numbers are prefixed with {@code whatsapp:}.</p>
 *
 * <pre>{@code
 * TwilioConfig config = TwilioConfig.builder()
 *     .accountSid("ACXXXXXXXXXX")
 *     .authToken("your-token")
 *     .fromNumber("+14155238886")  // Twilio WhatsApp sandbox number
 *     .build();
 *
 * TwilioWhatsAppChannel whatsapp = new TwilioWhatsAppChannel(config);
 * }</pre>
 */
public class TwilioWhatsAppChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(TwilioWhatsAppChannel.class);

    private final TwilioConfig config;
    private volatile boolean initialized = false;

    public TwilioWhatsAppChannel(TwilioConfig config) {
        this.config = config;
    }

    @Override
    public String getName() {
        return "whatsapp";
    }

    @Override
    public void send(Notification notification) {
        ensureInitialized();

        try {
            String content = notification.getRenderedContent();

            // WhatsApp has a 4096 char limit
            if (content.length() > 4096) {
                log.warn("WhatsApp content truncated from {} to 4096 chars", content.length());
                content = content.substring(0, 4093) + "...";
            }

            String toNumber = "whatsapp:" + notification.getRecipient();
            String fromNumber = "whatsapp:" + config.getFromNumber();

            MessageCreator creator = Message.creator(
                    new PhoneNumber(toNumber),
                    new PhoneNumber(fromNumber),
                    content
            );

            // Support media via URL (Twilio fetches the image/video from the URL)
            Object mediaUrl = notification.getParams().get("mediaUrl");
            if (mediaUrl != null && !mediaUrl.toString().isEmpty()) {
                creator.setMediaUrl(List.of(URI.create(mediaUrl.toString())));
                log.debug("WhatsApp media attached: {}", mediaUrl);
            }

            Message message = creator.create();

            log.debug("WhatsApp sent to '{}', SID: {}", notification.getRecipient(), message.getSid());

        } catch (Exception e) {
            throw new NotificationSendException("whatsapp",
                    "Failed to send WhatsApp to '" + notification.getRecipient() + "': " + e.getMessage(), e);
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
            log.info("Twilio WhatsApp channel initialized");
        }
    }
}
