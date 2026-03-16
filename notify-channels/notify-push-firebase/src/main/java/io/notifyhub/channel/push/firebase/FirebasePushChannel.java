package io.notifyhub.channel.push.firebase;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Firebase Cloud Messaging push notification channel using the HTTP v1 API.
 *
 * <p>Sends push notifications via the FCM v1 API
 * ({@code /v1/projects/{projectId}/messages:send}).
 * No external SDK needed — uses the JDK {@link HttpClient}.</p>
 *
 * <pre>{@code
 * FirebasePushChannel push = new FirebasePushChannel(
 *     FirebasePushConfig.builder()
 *         .projectId("my-firebase-project")
 *         .serviceAccountJson("{...}")
 *         .build()
 * );
 * }</pre>
 */
public class FirebasePushChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(FirebasePushChannel.class);
    private static final String FCM_V1_API = "https://fcm.googleapis.com/v1/projects/";
    private static final String FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final Duration TOKEN_REFRESH_MARGIN = Duration.ofMinutes(5);

    private final FirebasePushConfig config;
    private final HttpClient httpClient;

    private volatile String cachedAccessToken;
    private volatile Instant tokenExpiresAt = Instant.EPOCH;

    public FirebasePushChannel(FirebasePushConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();
    }

    @Override
    public String getName() {
        return "push";
    }

    @Override
    public void send(Notification notification) {
        String token = notification.getRecipient();
        if (token == null || token.isBlank()) {
            throw new NotificationSendException("push",
                    "No device token (recipient) provided for push notification");
        }

        String content = notification.getRenderedContent();
        String subject = notification.getSubject();
        String title = (subject != null && !subject.isBlank()) ? subject : "Notification";
        String imageUrl = notification.getImageUrl();

        try {
            String accessToken = getAccessToken();

            StringBuilder sb = new StringBuilder();
            sb.append("{\"message\": {\"token\": \"").append(escapeJson(token)).append("\", ");
            sb.append("\"notification\": {\"title\": \"").append(escapeJson(title)).append("\"");
            sb.append(", \"body\": \"").append(escapeJson(content)).append("\"");
            if (imageUrl != null && !imageUrl.isBlank()) {
                sb.append(", \"image\": \"").append(escapeJson(imageUrl)).append("\"");
            }
            sb.append("}}}");
            String payload = sb.toString();

            String url = FCM_V1_API + config.getProjectId() + "/messages:send";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + accessToken)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new NotificationSendException("push",
                        "FCM API returned " + response.statusCode() + ": " + response.body());
            }

            log.debug("FCM push sent to token '{}': {}", token, response.body());

        } catch (NotificationSendException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationSendException("push",
                    "Failed to send FCM push notification: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        return config.getProjectId() != null && !config.getProjectId().isBlank()
                && config.getServiceAccountJson() != null && !config.getServiceAccountJson().isBlank();
    }

    private synchronized String getAccessToken() throws Exception {
        if (cachedAccessToken != null && Instant.now().isBefore(tokenExpiresAt.minus(TOKEN_REFRESH_MARGIN))) {
            return cachedAccessToken;
        }

        String json = config.getServiceAccountJson();
        String clientEmail = extractJsonString(json, "client_email");
        String privateKeyPem = extractJsonString(json, "private_key");
        String tokenUri = extractJsonString(json, "token_uri");
        if (tokenUri == null || tokenUri.isBlank()) {
            tokenUri = "https://oauth2.googleapis.com/token";
        }

        Instant now = Instant.now();
        Instant exp = now.plusSeconds(3600);

        String jwt = buildSignedJwt(clientEmail, FCM_SCOPE, tokenUri, now, exp, privateKeyPem);

        String body = "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=" + jwt;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUri))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new NotificationSendException("push",
                    "Failed to obtain FCM access token: " + response.statusCode() + " " + response.body());
        }

        String responseBody = response.body();
        cachedAccessToken = extractJsonString(responseBody, "access_token");
        String expiresIn = extractJsonString(responseBody, "expires_in");
        if (expiresIn != null) {
            tokenExpiresAt = now.plusSeconds(Long.parseLong(expiresIn.trim()));
        } else {
            tokenExpiresAt = exp;
        }

        log.debug("FCM OAuth2 access token refreshed, expires at {}", tokenExpiresAt);
        return cachedAccessToken;
    }

    private String buildSignedJwt(String clientEmail, String scope, String audience,
                                   Instant iat, Instant exp, String privateKeyPem) throws Exception {
        String header = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
        String claims = "{\"iss\":\"" + escapeJson(clientEmail) + "\","
                + "\"scope\":\"" + scope + "\","
                + "\"aud\":\"" + escapeJson(audience) + "\","
                + "\"iat\":" + iat.getEpochSecond() + ","
                + "\"exp\":" + exp.getEpochSecond() + "}";

        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String headerB64 = encoder.encodeToString(header.getBytes());
        String claimsB64 = encoder.encodeToString(claims.getBytes());
        String signingInput = headerB64 + "." + claimsB64;

        RSAPrivateKey rsaKey = parsePrivateKey(privateKeyPem);
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(rsaKey);
        sig.update(signingInput.getBytes());
        String signatureB64 = encoder.encodeToString(sig.sign());

        return signingInput + "." + signatureB64;
    }

    private RSAPrivateKey parsePrivateKey(String pem) throws Exception {
        String stripped = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\n", "")
                .replace("\n", "")
                .replace("\r", "")
                .replace(" ", "");
        byte[] decoded = Base64.getDecoder().decode(stripped);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) kf.generatePrivate(spec);
    }

    private String extractJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colonIdx = json.indexOf(':', idx + search.length());
        if (colonIdx < 0) return null;
        int start = json.indexOf('"', colonIdx + 1);
        if (start < 0) {
            // Might be a numeric value (like expires_in)
            int numStart = colonIdx + 1;
            while (numStart < json.length() && (json.charAt(numStart) == ' ' || json.charAt(numStart) == '\t')) {
                numStart++;
            }
            int numEnd = numStart;
            while (numEnd < json.length() && Character.isDigit(json.charAt(numEnd))) {
                numEnd++;
            }
            if (numEnd > numStart) {
                return json.substring(numStart, numEnd);
            }
            return null;
        }
        int end = start + 1;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == '\\') { end += 2; continue; }
            if (c == '"') break;
            end++;
        }
        return json.substring(start + 1, end);
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
