package io.notifyhub.checkout.controller;

import io.notifyhub.checkout.service.LicenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/license")
@CrossOrigin(origins = "*")
public class LicenseController {

    private final LicenseService licenseService;

    public LicenseController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    /**
     * GET /api/license/validate?key=NH-PRO-XXXXXXXX-XXXXXXXX-XXXXXXXX
     * Returns: { "valid": true, "plan": "pro", "email": "...", "expiresAt": "..." }
     * Or:      { "valid": false }
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestParam String key) {
        return licenseService.validate(key)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(Map.of("valid", false)));
    }
}
