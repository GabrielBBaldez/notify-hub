package io.notifyhub.checkout.model;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LicenseKeyRepository extends JpaRepository<LicenseKey, Long> {

    Optional<LicenseKey> findByLicenseKey(String licenseKey);

    Optional<LicenseKey> findByStripeSessionId(String stripeSessionId);

    boolean existsByStripeSessionId(String stripeSessionId);
}
