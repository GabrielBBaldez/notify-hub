package io.notifyhub.channel.youtube;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationSendException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class YouTubeChannelTest {

    @Test
    void getName_returnsYoutube() {
        YouTubeConfig config = YouTubeConfig.builder()
                .accessToken("test-token")
                .build();
        YouTubeChannel channel = new YouTubeChannel(config);
        assertEquals("youtube", channel.getName());
    }

    @Test
    void isAvailable_returnsTrueWhenConfigured() {
        YouTubeConfig config = YouTubeConfig.builder()
                .accessToken("test-token")
                .build();
        YouTubeChannel channel = new YouTubeChannel(config);
        assertTrue(channel.isAvailable());
    }

    @Test
    void config_requiresAccessToken() {
        assertThrows(IllegalArgumentException.class, () ->
                YouTubeConfig.builder().build());
    }

    @Test
    void config_acceptsOptionalFields() {
        YouTubeConfig config = YouTubeConfig.builder()
                .accessToken("test-token")
                .channelId("UC123")
                .liveChatId("chat123")
                .timeoutMs(5000)
                .build();
        assertEquals("test-token", config.getAccessToken());
        assertEquals("UC123", config.getChannelId());
        assertEquals("chat123", config.getLiveChatId());
        assertEquals(5000, config.getTimeoutMs());
    }

    @Test
    void send_failsWithNetworkError() {
        YouTubeConfig config = YouTubeConfig.builder()
                .accessToken("test-token")
                .liveChatId("chat123")
                .timeoutMs(100)
                .build();
        YouTubeChannel channel = new YouTubeChannel(config);

        Notification notification = new Notification(
                null, "default", null, null, "Hello YouTube!", Map.of());

        assertThrows(NotificationSendException.class,
                () -> channel.send(notification));
    }

    @Test
    void poll_failsWithTooFewOptions() {
        YouTubeConfig config = YouTubeConfig.builder()
                .accessToken("test-token")
                .liveChatId("chat123")
                .build();
        YouTubeChannel channel = new YouTubeChannel(config);

        Notification notification = new Notification(
                null, "default", null, null, "Question?",
                Map.of("poll_options", List.of("Only one")));

        NotificationSendException ex = assertThrows(NotificationSendException.class,
                () -> channel.send(notification));
        assertTrue(ex.getMessage().contains("at least 2"));
    }

    @Test
    void poll_failsWithTooManyOptions() {
        YouTubeConfig config = YouTubeConfig.builder()
                .accessToken("test-token")
                .liveChatId("chat123")
                .build();
        YouTubeChannel channel = new YouTubeChannel(config);

        Notification notification = new Notification(
                null, "default", null, null, "Question?",
                Map.of("poll_options", List.of("A", "B", "C", "D", "E")));

        NotificationSendException ex = assertThrows(NotificationSendException.class,
                () -> channel.send(notification));
        assertTrue(ex.getMessage().contains("at most 4"));
    }

    @Test
    void poll_usesContentAsFallbackQuestion() {
        YouTubeConfig config = YouTubeConfig.builder()
                .accessToken("test-token")
                .liveChatId("chat123")
                .timeoutMs(100)
                .build();
        YouTubeChannel channel = new YouTubeChannel(config);

        // Poll with content but no explicit poll_question — should use content as question
        // Will fail with network error but validates that no "question required" exception is thrown
        Notification notification = new Notification(
                null, "default", null, null, "Qual framework?",
                Map.of("poll_options", List.of("Spring", "Quarkus")));

        NotificationSendException ex = assertThrows(NotificationSendException.class,
                () -> channel.send(notification));
        // Should be a network error, not a validation error
        assertFalse(ex.getMessage().contains("question"));
    }

    @Test
    void poll_usesExplicitPollQuestion() {
        YouTubeConfig config = YouTubeConfig.builder()
                .accessToken("test-token")
                .liveChatId("chat123")
                .timeoutMs(100)
                .build();
        YouTubeChannel channel = new YouTubeChannel(config);

        Notification notification = new Notification(
                null, "default", null, null, "ignored content",
                Map.of("poll_options", List.of("A", "B"),
                       "poll_question", "Explicit question?"));

        NotificationSendException ex = assertThrows(NotificationSendException.class,
                () -> channel.send(notification));
        // Should be a network error, not a validation error
        assertFalse(ex.getMessage().contains("question"));
    }

    @Test
    void poll_withValidOptionsFailsOnlyOnNetwork() {
        YouTubeConfig config = YouTubeConfig.builder()
                .accessToken("test-token")
                .liveChatId("chat123")
                .timeoutMs(100)
                .build();
        YouTubeChannel channel = new YouTubeChannel(config);

        // 4 options (max allowed)
        Notification notification = new Notification(
                null, "default", null, null, "Pick one",
                Map.of("poll_options", List.of("A", "B", "C", "D")));

        NotificationSendException ex = assertThrows(NotificationSendException.class,
                () -> channel.send(notification));
        // Network error, not validation
        assertTrue(ex.getMessage().contains("Failed") || ex.getMessage().contains("YouTube API"));
    }

    @Test
    void config_buildsWithRefreshCredentials() {
        YouTubeConfig config = YouTubeConfig.builder()
                .accessToken("ya29.seed")
                .refreshToken("1//refresh")
                .clientId("client-id")
                .clientSecret("client-secret")
                .build();
        assertTrue(config.hasTokenRefresh());
        // Initial token is the seed
        assertEquals("ya29.seed", config.getAccessToken());
    }

    @Test
    void config_buildsWithStaticTokenOnly() {
        YouTubeConfig config = YouTubeConfig.builder()
                .accessToken("ya29.static")
                .build();
        assertFalse(config.hasTokenRefresh());
        assertEquals("ya29.static", config.getAccessToken());
    }

    @Test
    void config_failsWithoutTokenOrRefreshCredentials() {
        assertThrows(IllegalArgumentException.class, () ->
                YouTubeConfig.builder().build());
    }

    @Test
    void config_partialRefreshCredentials_fallsBackToStaticToken() {
        // Only refreshToken without clientId/clientSecret → falls back to static
        YouTubeConfig config = YouTubeConfig.builder()
                .accessToken("ya29.static")
                .refreshToken("1//refresh")
                .build();
        assertFalse(config.hasTokenRefresh());
        assertEquals("ya29.static", config.getAccessToken());
    }

    @Test
    void textMessage_notAffectedByPollFeature() {
        YouTubeConfig config = YouTubeConfig.builder()
                .accessToken("test-token")
                .liveChatId("chat123")
                .timeoutMs(100)
                .build();
        YouTubeChannel channel = new YouTubeChannel(config);

        // Normal text message (no poll_options) — should still work as before
        Notification notification = new Notification(
                null, "default", null, null, "Just a text message", Map.of());

        NotificationSendException ex = assertThrows(NotificationSendException.class,
                () -> channel.send(notification));
        // Network error, not poll-related
        assertTrue(ex.getMessage().contains("Failed") || ex.getMessage().contains("YouTube API"));
    }
}
