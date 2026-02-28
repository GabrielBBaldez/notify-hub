package io.notifyhub.checkout.service;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

    private static final Logger log = LoggerFactory.getLogger(StripeService.class);

    @Value("${stripe.secret-key}")
    private String secretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    @Value("${stripe.pro-price-id}")
    private String proPriceId;

    @PostConstruct
    void init() {
        Stripe.apiKey = secretKey;
        log.info("Stripe initialized (key ending in ...{})", secretKey.substring(secretKey.length() - 4));
    }

    /**
     * Create a Stripe Checkout Session for the given plan.
     */
    public Session createCheckoutSession(String plan, String customerEmail) throws StripeException {
        String priceId = proPriceId; // For now, single plan

        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .setCustomerEmail(customerEmail)
                .putMetadata("plan", plan)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPrice(priceId)
                                .setQuantity(1L)
                                .build()
                );

        Session session = Session.create(builder.build());
        log.info("Checkout session created: id={}, plan={}, email={}", session.getId(), plan, customerEmail);
        return session;
    }

    /**
     * Verify and parse a Stripe webhook event.
     */
    public Event verifyWebhookEvent(String payload, String sigHeader) throws SignatureVerificationException {
        return Webhook.constructEvent(payload, sigHeader, webhookSecret);
    }

    /**
     * Retrieve a checkout session by ID.
     */
    public Session retrieveSession(String sessionId) throws StripeException {
        return Session.retrieve(sessionId);
    }
}
