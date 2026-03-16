package io.notifyhub.channel.push.firebase;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationSendException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FirebasePushChannelTest {

    private static final String TEST_PROJECT_ID = "test-project";
    private static final String TEST_SERVICE_ACCOUNT = "{\"client_email\":\"test@test.iam.gserviceaccount.com\",\"private_key\":\"test\",\"token_uri\":\"https://oauth2.googleapis.com/token\"}";

    private FirebasePushConfig testConfig() {
        return FirebasePushConfig.builder()
                .projectId(TEST_PROJECT_ID)
                .serviceAccountJson(TEST_SERVICE_ACCOUNT)
                .build();
    }

    @Test
    @DisplayName("getName() returns 'push'")
    void getName() {
        FirebasePushChannel channel = new FirebasePushChannel(testConfig());
        assertEquals("push", channel.getName());
    }

    @Test
    @DisplayName("isAvailable() returns true when project ID and service account are set")
    void isAvailable() {
        FirebasePushChannel channel = new FirebasePushChannel(testConfig());
        assertTrue(channel.isAvailable());
    }

    @Test
    @DisplayName("Config requires non-blank project ID")
    void configValidationProjectId() {
        assertThrows(IllegalArgumentException.class, () ->
                FirebasePushConfig.builder()
                        .projectId("")
                        .serviceAccountJson(TEST_SERVICE_ACCOUNT)
                        .build());
        assertThrows(IllegalArgumentException.class, () ->
                FirebasePushConfig.builder()
                        .serviceAccountJson(TEST_SERVICE_ACCOUNT)
                        .build());
    }

    @Test
    @DisplayName("Config requires non-blank service account JSON")
    void configValidationServiceAccount() {
        assertThrows(IllegalArgumentException.class, () ->
                FirebasePushConfig.builder()
                        .projectId(TEST_PROJECT_ID)
                        .serviceAccountJson("")
                        .build());
        assertThrows(IllegalArgumentException.class, () ->
                FirebasePushConfig.builder()
                        .projectId(TEST_PROJECT_ID)
                        .build());
    }

    @Test
    @DisplayName("Config uses default timeout of 10000ms")
    void configDefaultTimeout() {
        FirebasePushConfig config = testConfig();
        assertEquals(10_000, config.getTimeoutMs());
    }

    @Test
    @DisplayName("Config allows custom timeout")
    void configCustomTimeout() {
        FirebasePushConfig config = FirebasePushConfig.builder()
                .projectId(TEST_PROJECT_ID)
                .serviceAccountJson(TEST_SERVICE_ACCOUNT)
                .timeoutMs(5_000)
                .build();
        assertEquals(5_000, config.getTimeoutMs());
    }

    @Test
    @DisplayName("send() throws when no device token (recipient) provided")
    void sendFailsWithoutRecipient() {
        FirebasePushChannel channel = new FirebasePushChannel(testConfig());

        Notification notification = new Notification(
                "", "push", "Test Title", null, "Test message", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }

    @Test
    @DisplayName("send() throws when recipient is null")
    void sendFailsWithNullRecipient() {
        FirebasePushChannel channel = new FirebasePushChannel(testConfig());

        Notification notification = new Notification(
                null, "push", "Test Title", null, "Test message", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }

    @Test
    @DisplayName("send() throws NotificationSendException on invalid service account")
    void sendFailsOnInvalidServiceAccount() {
        FirebasePushChannel channel = new FirebasePushChannel(testConfig());

        Notification notification = new Notification(
                "device-token-123", "push", "Test Title", null, "Test message", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }
}
