package io.notifyhub.core.oauth;

/**
 * Thrown when an OAuth 2.0 token refresh fails.
 *
 * <p>This may indicate invalid refresh credentials, revoked tokens,
 * or network issues with the OAuth provider.</p>
 */
public class OAuthTokenRefreshException extends RuntimeException {

    public OAuthTokenRefreshException(String message) {
        super(message);
    }

    public OAuthTokenRefreshException(String message, Throwable cause) {
        super(message, cause);
    }
}
