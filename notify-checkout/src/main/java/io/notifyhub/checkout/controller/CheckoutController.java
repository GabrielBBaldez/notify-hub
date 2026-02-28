package io.notifyhub.checkout.controller;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import io.notifyhub.checkout.service.StripeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CheckoutController {

    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);

    private final StripeService stripeService;

    public CheckoutController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    /**
     * POST /api/checkout
     * Body: { "plan": "pro", "email": "user@example.com" }
     * Returns: { "sessionId": "cs_...", "url": "https://checkout.stripe.com/..." }
     */
    @PostMapping("/checkout")
    public ResponseEntity<Map<String, String>> createCheckout(@RequestBody Map<String, String> body) {
        String plan = body.getOrDefault("plan", "pro");
        String email = body.get("email");

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "email is required"));
        }

        try {
            Session session = stripeService.createCheckoutSession(plan, email);
            return ResponseEntity.ok(Map.of(
                    "sessionId", session.getId(),
                    "url", session.getUrl()
            ));
        } catch (StripeException e) {
            log.error("Stripe checkout error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
