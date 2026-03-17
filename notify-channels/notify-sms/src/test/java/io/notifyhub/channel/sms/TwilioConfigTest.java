package io.notifyhub.channel.sms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TwilioConfigTest {

    @Test
    @DisplayName("Builder creates config with all fields")
    void builderCreatesConfig() {
        TwilioConfig config = TwilioConfig.builder()
                .accountSid("ACtest000000000000000000000000000")
                .authToken("test-token")
                .fromNumber("+15551234567")
                .build();
        assertEquals("ACtest000000000000000000000000000", config.getAccountSid());
        assertEquals("test-token", config.getAuthToken());
        assertEquals("+15551234567", config.getFromNumber());
    }

    @Test
    @DisplayName("Builder throws on null accountSid")
    void requiresAccountSid() {
        assertThrows(IllegalArgumentException.class, () ->
                TwilioConfig.builder().authToken("token").fromNumber("+15551234567").build());
    }

    @Test
    @DisplayName("Builder throws on blank accountSid")
    void requiresNonBlankAccountSid() {
        assertThrows(IllegalArgumentException.class, () ->
                TwilioConfig.builder().accountSid("").authToken("token").fromNumber("+15551234567").build());
    }

    @Test
    @DisplayName("Builder throws on null authToken")
    void requiresAuthToken() {
        assertThrows(IllegalArgumentException.class, () ->
                TwilioConfig.builder().accountSid("ACtest").fromNumber("+15551234567").build());
    }

    @Test
    @DisplayName("Builder throws on null fromNumber")
    void requiresFromNumber() {
        assertThrows(IllegalArgumentException.class, () ->
                TwilioConfig.builder().accountSid("ACtest").authToken("token").build());
    }
}
