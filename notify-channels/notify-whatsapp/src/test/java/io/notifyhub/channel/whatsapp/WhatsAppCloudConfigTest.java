package io.notifyhub.channel.whatsapp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WhatsAppCloudConfigTest {

    @Test
    @DisplayName("Should build config with all fields")
    void shouldBuildWithAllFields() {
        Map<String, String> recipients = Map.of("john", "+5548999999999");

        WhatsAppCloudConfig config = WhatsAppCloudConfig.builder()
                .accessToken("EAAGm0PX4ZCps...")
                .phoneNumberId("106540012345678")
                .recipients(recipients)
                .timeoutMs(5_000)
                .apiVersion("v22.0")
                .build();

        assertEquals("EAAGm0PX4ZCps...", config.getAccessToken());
        assertEquals("106540012345678", config.getPhoneNumberId());
        assertEquals(Map.of("john", "+5548999999999"), config.getRecipients());
        assertEquals(5_000, config.getTimeoutMs());
        assertEquals("v22.0", config.getApiVersion());
    }

    @Test
    @DisplayName("Should use default timeoutMs of 10000 and apiVersion v21.0")
    void shouldUseDefaults() {
        WhatsAppCloudConfig config = WhatsAppCloudConfig.builder()
                .accessToken("token")
                .phoneNumberId("phone123")
                .build();

        assertEquals(10_000, config.getTimeoutMs());
        assertEquals("v21.0", config.getApiVersion());
        assertTrue(config.getRecipients().isEmpty());
    }

    @Test
    @DisplayName("Should throw when accessToken is blank")
    void shouldThrowWhenAccessTokenBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                WhatsAppCloudConfig.builder()
                        .accessToken("")
                        .phoneNumberId("phone123")
                        .build());
    }

    @Test
    @DisplayName("Should throw when accessToken is null")
    void shouldThrowWhenAccessTokenNull() {
        assertThrows(IllegalArgumentException.class, () ->
                WhatsAppCloudConfig.builder()
                        .phoneNumberId("phone123")
                        .build());
    }

    @Test
    @DisplayName("Should throw when phoneNumberId is blank")
    void shouldThrowWhenPhoneNumberIdBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                WhatsAppCloudConfig.builder()
                        .accessToken("token")
                        .phoneNumberId("")
                        .build());
    }

    @Test
    @DisplayName("Should throw when phoneNumberId is null")
    void shouldThrowWhenPhoneNumberIdNull() {
        assertThrows(IllegalArgumentException.class, () ->
                WhatsAppCloudConfig.builder()
                        .accessToken("token")
                        .build());
    }

    @Test
    @DisplayName("Recipients map should be unmodifiable")
    void recipientsShouldBeUnmodifiable() {
        WhatsAppCloudConfig config = WhatsAppCloudConfig.builder()
                .accessToken("token")
                .phoneNumberId("phone123")
                .recipients(Map.of("a", "b"))
                .build();

        assertThrows(UnsupportedOperationException.class, () ->
                config.getRecipients().put("new", "value"));
    }
}
