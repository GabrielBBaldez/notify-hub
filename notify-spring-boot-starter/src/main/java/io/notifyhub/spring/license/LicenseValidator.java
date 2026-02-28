package io.notifyhub.spring.license;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Validates NotifyHub license keys against the checkout API.
 * <p>
 * Free tier (no license key): email channel only.
 * Pro tier (NH-PRO-*): all channels, all features.
 * Enterprise tier (NH-ENT-*): all channels, all features, priority support.
 */
public class LicenseValidator {

    private static final Logger log = LoggerFactory.getLogger(LicenseValidator.class);
    private static final String VALIDATION_URL = "https://notifyhub-checkout.up.railway.app/api/license/validate?key=";

    private final String licenseKey;
    private String plan = "free";
    private boolean valid = false;

    public LicenseValidator(String licenseKey) {
        this.licenseKey = licenseKey;
        if (licenseKey != null && !licenseKey.isBlank()) {
            validate();
        } else {
            log.info("NotifyHub: Running in FREE tier (no license key). Limited to email channel.");
        }
    }

    private void validate() {
        // Offline format check first
        if (!licenseKey.matches("NH-(PRO|ENT)-[A-F0-9]{8}-[A-F0-9]{8}-[A-F0-9]{8}")) {
            log.warn("NotifyHub: Invalid license key format");
            return;
        }

        // Try online validation using java.net (no Spring Web dependency needed)
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(VALIDATION_URL + licenseKey).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String body = reader.lines().reduce("", String::concat);
                    if (body.contains("\"valid\":true") || body.contains("\"valid\": true")) {
                        this.valid = true;
                        this.plan = extractJsonValue(body, "plan");
                        if (this.plan == null) this.plan = "pro";
                        String expiresAt = extractJsonValue(body, "expiresAt");
                        log.info("NotifyHub: License validated — {} tier (expires: {})",
                                plan.toUpperCase(), expiresAt);
                    } else {
                        acceptOffline("Online validation returned invalid");
                    }
                }
            } else {
                acceptOffline("Server returned status " + conn.getResponseCode());
            }
        } catch (Exception e) {
            acceptOffline(e.getMessage());
        }
    }

    private void acceptOffline(String reason) {
        log.warn("NotifyHub: {} — accepting key format offline", reason);
        this.valid = true;
        this.plan = licenseKey.startsWith("NH-ENT") ? "enterprise" : "pro";
    }

    private static String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) {
            search = "\"" + key + "\": \"";
            start = json.indexOf(search);
        }
        if (start < 0) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        return end > start ? json.substring(start, end) : null;
    }

    public boolean isValid() { return valid; }
    public String getPlan() { return plan; }
    public boolean isPro() { return valid && ("pro".equals(plan) || "enterprise".equals(plan)); }
    public boolean isEnterprise() { return valid && "enterprise".equals(plan); }
    public boolean isFree() { return !valid; }
}
