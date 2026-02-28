package io.notifyhub.checkout.service;

import io.notifyhub.checkout.model.LicenseKey;
import io.notifyhub.checkout.model.LicenseKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

@Service
public class LicenseService {

    private static final Logger log = LoggerFactory.getLogger(LicenseService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final LicenseKeyRepository repository;

    public LicenseService(LicenseKeyRepository repository) {
        this.repository = repository;
    }

    /**
     * Generate a new license key after successful Stripe payment.
     */
    public LicenseKey createLicense(String email, String plan, String stripeSessionId, String stripeCustomerId) {
        // Prevent duplicate processing
        if (repository.existsByStripeSessionId(stripeSessionId)) {
            log.warn("License already exists for session {}", stripeSessionId);
            return repository.findByStripeSessionId(stripeSessionId).orElseThrow();
        }

        LicenseKey license = new LicenseKey();
        license.setLicenseKey(generateKey(plan));
        license.setEmail(email);
        license.setPlan(plan);
        license.setStripeSessionId(stripeSessionId);
        license.setStripeCustomerId(stripeCustomerId);
        license.setActive(true);
        license.setExpiresAt(Instant.now().plus(365, ChronoUnit.DAYS));

        LicenseKey saved = repository.save(license);
        log.info("License created: plan={}, email={}, key={}...", plan, email, saved.getLicenseKey().substring(0, 12));
        return saved;
    }

    /**
     * Validate a license key. Returns plan info if valid.
     */
    public Optional<Map<String, Object>> validate(String key) {
        return repository.findByLicenseKey(key)
                .filter(LicenseKey::isActive)
                .filter(l -> l.getExpiresAt() == null || l.getExpiresAt().isAfter(Instant.now()))
                .map(l -> Map.<String, Object>of(
                        "valid", true,
                        "plan", l.getPlan(),
                        "email", l.getEmail(),
                        "expiresAt", l.getExpiresAt().toString()
                ));
    }

    /**
     * Generate a formatted license key: NH-PRO-XXXXXXXX-XXXXXXXX-XXXXXXXX
     */
    private String generateKey(String plan) {
        String prefix = "enterprise".equals(plan) ? "NH-ENT" : "NH-PRO";
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        String hex = HexFormat.of().formatHex(bytes).toUpperCase();
        return prefix + "-" + hex.substring(0, 8) + "-" + hex.substring(8, 16) + "-" + hex.substring(16, 24);
    }
}
