package io.notifyhub.core.oauth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OAuthTokenManagerTest {

    @Test
    void builder_requiresTokenEndpoint() {
        assertThrows(IllegalArgumentException.class, () ->
                OAuthTokenManager.builder()
                        .refreshToken("refresh")
                        .clientId("id")
                        .clientSecret("secret")
                        .build());
    }

    @Test
    void builder_requiresRefreshToken() {
        assertThrows(IllegalArgumentException.class, () ->
                OAuthTokenManager.builder()
                        .tokenEndpoint("https://example.com/token")
                        .clientId("id")
                        .clientSecret("secret")
                        .build());
    }

    @Test
    void builder_requiresClientId() {
        assertThrows(IllegalArgumentException.class, () ->
                OAuthTokenManager.builder()
                        .tokenEndpoint("https://example.com/token")
                        .refreshToken("refresh")
                        .clientSecret("secret")
                        .build());
    }

    @Test
    void builder_requiresClientSecret() {
        assertThrows(IllegalArgumentException.class, () ->
                OAuthTokenManager.builder()
                        .tokenEndpoint("https://example.com/token")
                        .refreshToken("refresh")
                        .clientId("id")
                        .build());
    }

    @Test
    void extractJsonString_parsesAccessToken() {
        String json = "{\"access_token\":\"ya29.newtoken\",\"expires_in\":3600,\"token_type\":\"Bearer\"}";
        assertEquals("ya29.newtoken", OAuthTokenManager.extractJsonString(json, "access_token"));
    }

    @Test
    void extractJsonString_parsesTokenType() {
        String json = "{\"access_token\":\"token\",\"token_type\":\"Bearer\"}";
        assertEquals("Bearer", OAuthTokenManager.extractJsonString(json, "token_type"));
    }

    @Test
    void extractJsonString_returnsNullForMissingKey() {
        String json = "{\"access_token\":\"token\"}";
        assertNull(OAuthTokenManager.extractJsonString(json, "refresh_token"));
    }

    @Test
    void extractJsonLong_parsesExpiresIn() {
        String json = "{\"access_token\":\"token\",\"expires_in\":3600}";
        assertEquals(3600, OAuthTokenManager.extractJsonLong(json, "expires_in", 0));
    }

    @Test
    void extractJsonLong_returnsDefaultForMissingKey() {
        String json = "{\"access_token\":\"token\"}";
        assertEquals(7200, OAuthTokenManager.extractJsonLong(json, "expires_in", 7200));
    }

    @Test
    void extractJsonLong_handlesWhitespace() {
        String json = "{\"expires_in\":  3600}";
        assertEquals(3600, OAuthTokenManager.extractJsonLong(json, "expires_in", 0));
    }

    @Test
    void getAccessToken_returnsInitialToken_whenFresh() {
        // Build with a valid initial token — it should return it without network call
        OAuthTokenManager manager = OAuthTokenManager.builder()
                .tokenEndpoint("https://oauth2.googleapis.com/token")
                .refreshToken("test-refresh")
                .clientId("test-client")
                .clientSecret("test-secret")
                .initialAccessToken("ya29.initial")
                .build();

        // Token should be returned immediately (within 1h validity)
        assertEquals("ya29.initial", manager.getAccessToken());
    }

    @Test
    void getAccessToken_throwsException_whenNoInitialAndBadEndpoint() {
        // No initial token + invalid endpoint → should attempt refresh and fail
        OAuthTokenManager manager = OAuthTokenManager.builder()
                .tokenEndpoint("https://localhost:1/nonexistent")
                .refreshToken("test-refresh")
                .clientId("test-client")
                .clientSecret("test-secret")
                .timeoutSeconds(1)
                .build();

        assertThrows(OAuthTokenRefreshException.class, manager::getAccessToken);
    }

    @Test
    void forceRefresh_throwsOnBadEndpoint() {
        OAuthTokenManager manager = OAuthTokenManager.builder()
                .tokenEndpoint("https://localhost:1/nonexistent")
                .refreshToken("test-refresh")
                .clientId("test-client")
                .clientSecret("test-secret")
                .initialAccessToken("ya29.initial")
                .timeoutSeconds(1)
                .build();

        assertThrows(OAuthTokenRefreshException.class, manager::forceRefresh);
    }

    @Test
    void getExpiresAt_isSetForInitialToken() {
        OAuthTokenManager manager = OAuthTokenManager.builder()
                .tokenEndpoint("https://oauth2.googleapis.com/token")
                .refreshToken("test-refresh")
                .clientId("test-client")
                .clientSecret("test-secret")
                .initialAccessToken("ya29.initial")
                .build();

        assertNotNull(manager.getExpiresAt());
    }

    @Test
    void getAccessToken_threadSafe_multipleReaders() throws InterruptedException {
        OAuthTokenManager manager = OAuthTokenManager.builder()
                .tokenEndpoint("https://oauth2.googleapis.com/token")
                .refreshToken("test-refresh")
                .clientId("test-client")
                .clientSecret("test-secret")
                .initialAccessToken("ya29.concurrent")
                .build();

        // Run 10 concurrent reads — all should return the same token
        Thread[] threads = new Thread[10];
        String[] results = new String[10];
        for (int i = 0; i < threads.length; i++) {
            int idx = i;
            threads[i] = new Thread(() -> results[idx] = manager.getAccessToken());
            threads[i].start();
        }
        for (Thread t : threads) t.join();

        for (String result : results) {
            assertEquals("ya29.concurrent", result);
        }
    }
}
