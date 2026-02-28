package io.notifyhub.checkout.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import io.notifyhub.checkout.model.LicenseKey;
import io.notifyhub.checkout.service.LicenseService;
import io.notifyhub.checkout.service.StripeService;
import io.notifyhub.core.Channel;
import io.notifyhub.core.NotifyHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/webhook")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeService stripeService;
    private final LicenseService licenseService;
    private final Optional<NotifyHub> notifyHub;

    public StripeWebhookController(StripeService stripeService,
                                    LicenseService licenseService,
                                    Optional<NotifyHub> notifyHub) {
        this.stripeService = stripeService;
        this.licenseService = licenseService;
        this.notifyHub = notifyHub;
    }

    /**
     * POST /api/webhook/stripe
     * Called by Stripe when payment completes.
     * Generates license key and sends it via email.
     */
    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = stripeService.verifyWebhookEvent(payload, sigHeader);
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe signature: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getDataObjectDeserializer()
                    .getObject().orElse(null);

            if (session == null) {
                log.error("Could not deserialize checkout session from event");
                return ResponseEntity.ok("ok");
            }

            String email = session.getCustomerEmail();
            String plan = session.getMetadata().getOrDefault("plan", "pro");
            String customerId = session.getCustomer();

            log.info("Payment completed: email={}, plan={}, session={}", email, plan, session.getId());

            // Generate license key
            LicenseKey license = licenseService.createLicense(email, plan, session.getId(), customerId);

            // Send license key via email using NotifyHub
            sendLicenseEmail(email, license.getLicenseKey(), plan);
        }

        return ResponseEntity.ok("ok");
    }

    private void sendLicenseEmail(String email, String licenseKey, String plan) {
        notifyHub.ifPresentOrElse(hub -> {
            try {
                hub.to(email)
                        .via(Channel.EMAIL)
                        .subject("Your NotifyHub " + plan.toUpperCase() + " License Key")
                        .content(buildLicenseEmailBody(licenseKey, plan))
                        .send();
                log.info("License email sent to {}", email);
            } catch (Exception e) {
                log.error("Failed to send license email to {}: {}", email, e.getMessage());
            }
        }, () -> log.warn("NotifyHub not configured — license key for {} not emailed: {}", email, licenseKey));
    }

    private String buildLicenseEmailBody(String licenseKey, String plan) {
        return """
                Thank you for purchasing NotifyHub %s!

                Your license key:
                %s

                To activate, add to your application.yml:

                notify:
                  license-key: %s

                This key is valid for 1 year from the date of purchase.

                Need help? Contact support@notifyhub.io

                — The NotifyHub Team
                """.formatted(plan.toUpperCase(), licenseKey, licenseKey);
    }
}
