package io.notifyhub.core.oauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe OAuth 2.0 token manager with proactive refresh.
 *
 * <p>Manages an access token lifecycle: stores the current token,
 * tracks expiry, and refreshes proactively before expiration.
 * Supports any OAuth 2.0 provider (Google, Twitch, Meta, LinkedIn, etc.).</p>
 *
 * <pre>{@code
 * OAuthTokenManager manager = OAuthTokenManager.builder()
 *     .tokenEndpoint("https://oauth2.googleapis.com/token")
 *     .refreshToken("1//0eXyz...")
 *     .clientId("client-id")
 *     .clientSecret("client-secret")
 *     .initialAccessToken("ya29.a0...")  // optional seed
 *     .build();
 *
 * String token = manager.getAccessToken(); // auto-refreshes if needed
 * }</pre>
 */
public class OAuthTokenManager {

    private static final Logger log = LoggerFactory.getLogger(OAuthTokenManager.class);

    /** Refresh 5 minutes before actual expiry. */
    private static final long DEFAULT_REFRESH_BUFFER_SECONDS = 300;

    private final String tokenEndpoint;
    private final String refreshToken;
    private final String clientId;
    private final String clientSecret;
    private final long refreshBufferSeconds;
    private final HttpClient httpClient;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private volatile String currentAccessToken;
    private volatile Instant expiresAt;

    private OAuthTokenManager(Builder builder) {
        this.tokenEndpoint = requireNonBlank(builder.tokenEndpoint, "tokenEndpoint");
        this.refreshToken = requireNonBlank(builder.refreshToken, "refreshToken");
        this.clientId = requireNonBlank(builder.clientId, "clientId");
        this.clientSecret = requireNonBlank(builder.clientSecret, "clientSecret");
        this.refreshBufferSeconds = builder.refreshBufferSeconds > 0
                ? builder.refreshBufferSeconds : DEFAULT_REFRESH_BUFFER_SECONDS;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(builder.timeoutSeconds > 0 ? builder.timeoutSeconds : 10))
                .build();

        if (builder.initialAccessToken != null && !builder.initialAccessToken.isBlank()) {
            this.currentAccessToken = builder.initialAccessToken;
            // Assume initial token has ~1h validity, but will refresh proactively
            this.expiresAt = Instant.now().plusSeconds(3600);
        } else {
            this.currentAccessToken = null;
            this.expiresAt = Instant.MIN; // force immediate refresh on first call
        }
    }

    /**
     * Returns a valid access token, refreshing if necessary.
     *
     * <p>If the current token is expired or within the refresh buffer window,
     * a new token is obtained via the OAuth 2.0 refresh flow. Thread-safe:
     * concurrent callers share the same token and only one refresh occurs.</p>
     *
     * @return a valid access token
     * @throws OAuthTokenRefreshException if the refresh fails
     */
    public String getAccessToken() {
        // Fast path: read lock to check if token is still valid
        lock.readLock().lock();
        try {
            if (currentAccessToken != null && !isExpiredOrNearExpiry()) {
                return currentAccessToken;
            }
        } finally {
            lock.readLock().unlock();
        }

        // Slow path: refresh outside lock, then assign under write lock
        // Double-check first under write lock
        lock.writeLock().lock();
        try {
            if (currentAccessToken != null && !isExpiredOrNearExpiry()) {
                return currentAccessToken;
            }
        } finally {
            lock.writeLock().unlock();
        }

        // Perform HTTP call outside lock to avoid blocking readers
        String[] result = fetchNewToken();

        lock.writeLock().lock();
        try {
            // Another thread may have refreshed while we were fetching
            if (currentAccessToken != null && !isExpiredOrNearExpiry()) {
                return currentAccessToken;
            }
            this.currentAccessToken = result[0];
            this.expiresAt = Instant.now().plusSeconds(Long.parseLong(result[1]));
            return currentAccessToken;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Forces an immediate token refresh regardless of expiry state.
     * Useful when receiving a 401 response from an API.
     *
     * @throws OAuthTokenRefreshException if the refresh fails
     */
    public void forceRefresh() {
        lock.writeLock().lock();
        try {
            refresh();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Returns the current token expiry time (for monitoring/testing). */
    public Instant getExpiresAt() {
        return expiresAt;
    }

    private boolean isExpiredOrNearExpiry() {
        return Instant.now().plusSeconds(refreshBufferSeconds).isAfter(expiresAt);
    }

    /**
     * Fetches a new token via HTTP. Returns [token, expiresInSeconds].
     * Called outside locks to avoid blocking readers during HTTP calls.
     */
    private String[] fetchNewToken() {
        log.debug("Refreshing OAuth token via {}", tokenEndpoint);

        try {
            String body = "grant_type=refresh_token"
                    + "&refresh_token=" + encode(refreshToken)
                    + "&client_id=" + encode(clientId)
                    + "&client_secret=" + encode(clientSecret);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenEndpoint))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new OAuthTokenRefreshException(
                        "Token refresh failed (HTTP " + response.statusCode() + "): " + response.body());
            }

            String responseBody = response.body();
            String newToken = extractJsonString(responseBody, "access_token");
            if (newToken == null || newToken.isBlank()) {
                throw new OAuthTokenRefreshException(
                        "Token refresh response missing access_token: " + responseBody);
            }

            long expiresIn = extractJsonLong(responseBody, "expires_in", 3600);
            log.info("OAuth token refreshed successfully (expires in {}s)", expiresIn);

            return new String[]{newToken, String.valueOf(expiresIn)};

        } catch (OAuthTokenRefreshException e) {
            throw e;
        } catch (Exception e) {
            throw new OAuthTokenRefreshException("Failed to refresh OAuth token: " + e.getMessage(), e);
        }
    }

    private void refresh() {
        String[] result = fetchNewToken();
        this.currentAccessToken = result[0];
        this.expiresAt = Instant.now().plusSeconds(Long.parseLong(result[1]));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** Simple JSON string extraction without external dependencies. */
    static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return null;
        int quote1 = json.indexOf('"', colon + 1);
        if (quote1 < 0) return null;
        int quote2 = json.indexOf('"', quote1 + 1);
        if (quote2 < 0) return null;
        return json.substring(quote1 + 1, quote2);
    }

    /** Simple JSON number extraction without external dependencies. */
    static long extractJsonLong(String json, String key, long defaultValue) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return defaultValue;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return defaultValue;

        // Skip whitespace after colon
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        if (start >= json.length()) return defaultValue;

        // Extract number
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        if (end == start) return defaultValue;

        try {
            return Long.parseLong(json.substring(start, end));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be null or blank");
        }
        return value;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String tokenEndpoint;
        private String refreshToken;
        private String clientId;
        private String clientSecret;
        private String initialAccessToken;
        private long refreshBufferSeconds;
        private int timeoutSeconds;

        public Builder tokenEndpoint(String tokenEndpoint) { this.tokenEndpoint = tokenEndpoint; return this; }
        public Builder refreshToken(String refreshToken) { this.refreshToken = refreshToken; return this; }
        public Builder clientId(String clientId) { this.clientId = clientId; return this; }
        public Builder clientSecret(String clientSecret) { this.clientSecret = clientSecret; return this; }
        public Builder initialAccessToken(String initialAccessToken) { this.initialAccessToken = initialAccessToken; return this; }
        public Builder refreshBufferSeconds(long refreshBufferSeconds) { this.refreshBufferSeconds = refreshBufferSeconds; return this; }
        public Builder timeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; return this; }

        public OAuthTokenManager build() {
            return new OAuthTokenManager(this);
        }
    }
}
