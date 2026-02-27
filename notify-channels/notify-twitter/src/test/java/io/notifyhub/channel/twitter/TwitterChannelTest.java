package io.notifyhub.channel.twitter;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationSendException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TwitterChannelTest {

    private static final String API_KEY = "test-api-key";
    private static final String API_SECRET = "test-api-secret";
    private static final String ACCESS_TOKEN = "test-access-token";
    private static final String ACCESS_TOKEN_SECRET = "test-access-token-secret";

    private TwitterConfig validConfig() {
        return TwitterConfig.builder()
                .apiKey(API_KEY)
                .apiSecret(API_SECRET)
                .accessToken(ACCESS_TOKEN)
                .accessTokenSecret(ACCESS_TOKEN_SECRET)
                .build();
    }

    @Test
    @DisplayName("getName() returns 'twitter'")
    void getName() {
        TwitterChannel channel = new TwitterChannel(validConfig());
        assertEquals("twitter", channel.getName());
    }

    @Test
    @DisplayName("isAvailable() returns true when API key is set")
    void isAvailable() {
        TwitterChannel channel = new TwitterChannel(validConfig());
        assertTrue(channel.isAvailable());
    }

    @Test
    @DisplayName("Config requires non-blank API key")
    void configRequiresApiKey() {
        assertThrows(IllegalArgumentException.class, () ->
                TwitterConfig.builder()
                        .apiKey("")
                        .apiSecret(API_SECRET)
                        .accessToken(ACCESS_TOKEN)
                        .accessTokenSecret(ACCESS_TOKEN_SECRET)
                        .build());
    }

    @Test
    @DisplayName("Config requires non-blank API secret")
    void configRequiresApiSecret() {
        assertThrows(IllegalArgumentException.class, () ->
                TwitterConfig.builder()
                        .apiKey(API_KEY)
                        .apiSecret("")
                        .accessToken(ACCESS_TOKEN)
                        .accessTokenSecret(ACCESS_TOKEN_SECRET)
                        .build());
    }

    @Test
    @DisplayName("Config requires non-blank access token")
    void configRequiresAccessToken() {
        assertThrows(IllegalArgumentException.class, () ->
                TwitterConfig.builder()
                        .apiKey(API_KEY)
                        .apiSecret(API_SECRET)
                        .accessToken("")
                        .accessTokenSecret(ACCESS_TOKEN_SECRET)
                        .build());
    }

    @Test
    @DisplayName("Config requires non-blank access token secret")
    void configRequiresAccessTokenSecret() {
        assertThrows(IllegalArgumentException.class, () ->
                TwitterConfig.builder()
                        .apiKey(API_KEY)
                        .apiSecret(API_SECRET)
                        .accessToken(ACCESS_TOKEN)
                        .accessTokenSecret("")
                        .build());
    }

    @Test
    @DisplayName("OAuth header is well-formed")
    void oauthHeader() {
        TwitterChannel channel = new TwitterChannel(validConfig());
        String header = channel.buildOAuthHeader("POST", "https://api.twitter.com/2/tweets");
        assertTrue(header.startsWith("OAuth "));
        assertTrue(header.contains("oauth_consumer_key"));
        assertTrue(header.contains("oauth_signature"));
        assertTrue(header.contains("oauth_nonce"));
        assertTrue(header.contains("oauth_timestamp"));
    }

    @Test
    @DisplayName("send() throws NotificationSendException on network error")
    void sendFailsOnNetworkError() {
        TwitterChannel channel = new TwitterChannel(
                TwitterConfig.builder()
                        .apiKey(API_KEY)
                        .apiSecret(API_SECRET)
                        .accessToken(ACCESS_TOKEN)
                        .accessTokenSecret(ACCESS_TOKEN_SECRET)
                        .timeoutMs(1000)
                        .build());

        Notification notification = new Notification(
                null, "twitter", null, null, "Test tweet", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }
}
