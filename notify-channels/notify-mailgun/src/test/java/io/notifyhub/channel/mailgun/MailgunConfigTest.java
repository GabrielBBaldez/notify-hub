package io.notifyhub.channel.mailgun;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MailgunConfig")
class MailgunConfigTest {

    @Test
    @DisplayName("Should build config with required fields")
    void shouldBuildWithRequiredFields() {
        MailgunConfig config = MailgunConfig.builder()
                .apiKey("key-abc123")
                .domain("mail.example.com")
                .from("noreply@example.com")
                .build();

        assertEquals("key-abc123", config.getApiKey());
        assertEquals("mail.example.com", config.getDomain());
        assertEquals("noreply@example.com", config.getFrom());
    }

    @Test
    @DisplayName("Should default region to US")
    void shouldDefaultRegionToUs() {
        MailgunConfig config = MailgunConfig.builder()
                .apiKey("key-abc123")
                .domain("mail.example.com")
                .from("noreply@example.com")
                .build();

        assertEquals("US", config.getRegion());
        assertEquals("https://api.mailgun.net", config.getBaseUrl());
    }

    @Test
    @DisplayName("Should return EU base URL when region is EU")
    void shouldReturnEuBaseUrl() {
        MailgunConfig config = MailgunConfig.builder()
                .apiKey("key-abc123")
                .domain("mail.example.com")
                .from("noreply@example.com")
                .region("EU")
                .build();

        assertEquals("EU", config.getRegion());
        assertEquals("https://api.eu.mailgun.net", config.getBaseUrl());
    }

    @Test
    @DisplayName("Should normalize region to uppercase")
    void shouldNormalizeRegionToUppercase() {
        MailgunConfig config = MailgunConfig.builder()
                .apiKey("key-abc123")
                .domain("mail.example.com")
                .from("noreply@example.com")
                .region("eu")
                .build();

        assertEquals("EU", config.getRegion());
        assertEquals("https://api.eu.mailgun.net", config.getBaseUrl());
    }

    @Test
    @DisplayName("Should default timeout to 10 seconds")
    void shouldDefaultTimeout() {
        MailgunConfig config = MailgunConfig.builder()
                .apiKey("key-abc123")
                .domain("mail.example.com")
                .from("noreply@example.com")
                .build();

        assertEquals(10_000, config.getTimeoutMs());
    }

    @Test
    @DisplayName("Should allow custom timeout")
    void shouldAllowCustomTimeout() {
        MailgunConfig config = MailgunConfig.builder()
                .apiKey("key-abc123")
                .domain("mail.example.com")
                .from("noreply@example.com")
                .timeoutMs(30_000)
                .build();

        assertEquals(30_000, config.getTimeoutMs());
    }

    @Test
    @DisplayName("Should throw when apiKey is blank")
    void shouldThrowWhenApiKeyBlank() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                MailgunConfig.builder()
                        .apiKey("")
                        .domain("mail.example.com")
                        .from("noreply@example.com")
                        .build());
        assertTrue(ex.getMessage().contains("API key"));
    }

    @Test
    @DisplayName("Should throw when domain is null")
    void shouldThrowWhenDomainNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                MailgunConfig.builder()
                        .apiKey("key-abc123")
                        .domain(null)
                        .from("noreply@example.com")
                        .build());
        assertTrue(ex.getMessage().contains("domain"));
    }

    @Test
    @DisplayName("Should throw when from is blank")
    void shouldThrowWhenFromBlank() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                MailgunConfig.builder()
                        .apiKey("key-abc123")
                        .domain("mail.example.com")
                        .from("  ")
                        .build());
        assertTrue(ex.getMessage().contains("from"));
    }
}
