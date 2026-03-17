package io.notifyhub.demo;

import io.notifyhub.core.Channel;
import io.notifyhub.core.NotifyHub;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * SMS and WhatsApp demo endpoints.
 */
@RestController
@Tag(name = "SMS & WhatsApp", description = "SMS and WhatsApp notifications via Twilio")
public class DemoSmsController {

    private final NotifyHub notify;

    public DemoSmsController(NotifyHub notify) {
        this.notify = notify;
    }

    // ===================== SMS (TWILIO) =====================

    @Operation(summary = "Send SMS", description = "Send SMS via Twilio")
    @PostMapping("/send/sms")
    public Map<String, String> sendSms(
            @RequestParam String to,
            @RequestParam(defaultValue = "Hello from NotifyHub! Your notification system is working.") String message) {

        notify.toPhone(to)
                .via(Channel.SMS)
                .content(message)
                .send();

        return Map.of(
                "status", "sent",
                "channel", "sms",
                "to", to,
                "tip", "Check your phone for the SMS!"
        );
    }

    // ===================== WHATSAPP (TWILIO) =====================

    @Operation(summary = "Send WhatsApp", description = "Send WhatsApp message via Twilio")
    @PostMapping("/send/whatsapp")
    public Map<String, String> sendWhatsApp(
            @RequestParam String to,
            @RequestParam(defaultValue = "Hello from NotifyHub via WhatsApp!") String message) {

        notify.toPhone(to)
                .via(Channel.WHATSAPP)
                .content(message)
                .send();

        return Map.of(
                "status", "sent",
                "channel", "whatsapp",
                "to", to,
                "tip", "Check your WhatsApp!"
        );
    }
}
