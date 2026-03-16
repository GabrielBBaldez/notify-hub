package io.notifyhub.channel.youtube;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * YouTube notification channel — sends live chat messages via YouTube Data API v3.
 *
 * <p>Sends messages and polls to YouTube live chat using
 * {@code POST https://www.googleapis.com/youtube/v3/liveChat/messages?part=snippet}.</p>
 *
 * <p>If no {@code liveChatId} is configured, automatically fetches it from the
 * active broadcast via {@code GET https://www.googleapis.com/youtube/v3/liveBroadcasts}.</p>
 *
 * <p>To create a poll, pass {@code poll_options} (and optionally {@code poll_question})
 * in the notification params. The poll question defaults to the rendered content.</p>
 */
public class YouTubeChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(YouTubeChannel.class);
    private static final String CHAT_URL = "https://www.googleapis.com/youtube/v3/liveChat/messages?part=snippet";
    private static final String BROADCASTS_URL = "https://www.googleapis.com/youtube/v3/liveBroadcasts?part=snippet&broadcastStatus=active&broadcastType=all";
    private static final int MAX_MESSAGE_LENGTH = 200;

    private final YouTubeConfig config;
    private final HttpClient httpClient;

    private volatile String cachedLiveChatId;
    private volatile Instant cacheTimestamp;
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    public YouTubeChannel(YouTubeConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();
    }

    @Override
    public String getName() {
        return "youtube";
    }

    @Override
    public void send(Notification notification) {
        String liveChatId = resolveLiveChatId(notification.getRecipient());
        List<String> pollOptions = extractPollOptions(notification);

        String payload;
        String logAction;

        if (pollOptions != null) {
            // Poll mode
            String question = extractPollQuestion(notification);
            validatePollOptions(pollOptions);
            payload = buildPollPayload(liveChatId, question, pollOptions);
            logAction = "poll";
        } else {
            // Text message mode
            String content = notification.getRenderedContent();
            if (content.length() > MAX_MESSAGE_LENGTH) {
                content = content.substring(0, MAX_MESSAGE_LENGTH - 3) + "...";
            }
            payload = buildTextPayload(liveChatId, content);
            logAction = "message";
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CHAT_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getAccessToken())
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new NotificationSendException("youtube",
                        "YouTube API returned " + response.statusCode() + ": " + response.body());
            }

            log.debug("YouTube live chat {} sent to {}: {}", logAction, liveChatId, response.statusCode());

        } catch (NotificationSendException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationSendException("youtube",
                    "Failed to send YouTube live chat " + logAction + ": " + e.getMessage(), e);
        }
    }

    private String buildTextPayload(String liveChatId, String content) {
        return "{" +
                "\"snippet\": {" +
                "\"liveChatId\": \"" + escapeJson(liveChatId) + "\", " +
                "\"type\": \"textMessageEvent\", " +
                "\"textMessageDetails\": {" +
                "\"messageText\": \"" + escapeJson(content) + "\"" +
                "}" +
                "}" +
                "}";
    }

    private String buildPollPayload(String liveChatId, String question, List<String> options) {
        StringBuilder optionsJson = new StringBuilder();
        for (int i = 0; i < options.size(); i++) {
            if (i > 0) optionsJson.append(", ");
            optionsJson.append("{\"optionText\": \"").append(escapeJson(options.get(i))).append("\"}");
        }

        return "{" +
                "\"snippet\": {" +
                "\"liveChatId\": \"" + escapeJson(liveChatId) + "\", " +
                "\"type\": \"pollEvent\", " +
                "\"pollDetails\": {" +
                "\"metadata\": {" +
                "\"questionText\": \"" + escapeJson(question) + "\", " +
                "\"options\": [" + optionsJson + "]" +
                "}" +
                "}" +
                "}" +
                "}";
    }

    @SuppressWarnings("unchecked")
    private List<String> extractPollOptions(Notification notification) {
        Map<String, Object> params = notification.getParams();
        if (params == null) return null;
        Object raw = params.get("poll_options");
        if (raw == null) return null;
        if (raw instanceof List<?>) {
            return ((List<?>) raw).stream().map(Object::toString).toList();
        }
        if (raw instanceof Collection<?>) {
            return ((Collection<?>) raw).stream().map(Object::toString).toList();
        }
        return null;
    }

    private String extractPollQuestion(Notification notification) {
        Map<String, Object> params = notification.getParams();
        if (params != null && params.get("poll_question") instanceof String q && !q.isBlank()) {
            return q;
        }
        String content = notification.getRenderedContent();
        if (content != null && !content.isBlank()) {
            return content;
        }
        throw new NotificationSendException("youtube",
                "Poll requires a question — set 'poll_question' param or provide content.");
    }

    private void validatePollOptions(List<String> options) {
        if (options.size() < 2) {
            throw new NotificationSendException("youtube",
                    "YouTube polls require at least 2 options, got " + options.size());
        }
        if (options.size() > 4) {
            throw new NotificationSendException("youtube",
                    "YouTube polls allow at most 4 options, got " + options.size());
        }
    }

    @Override
    public boolean isAvailable() {
        return config.getAccessToken() != null && !config.getAccessToken().isBlank();
    }

    @Override
    public Map<String, String> getConfiguredRecipients() {
        return config.getRecipients();
    }

    private String resolveLiveChatId(String recipient) {
        // Check named recipients map
        if (recipient != null && config.getRecipients().containsKey(recipient)) {
            return config.getRecipients().get(recipient);
        }
        // If recipient looks like a liveChatId (not "default"), use it directly
        if (recipient != null && !recipient.isBlank() && !"default".equals(recipient)) {
            return recipient;
        }
        // Use configured liveChatId
        if (config.getLiveChatId() != null && !config.getLiveChatId().isBlank()) {
            return config.getLiveChatId();
        }
        // Auto-fetch from active broadcast
        return fetchActiveLiveChatId();
    }

    private synchronized String fetchActiveLiveChatId() {
        if (cachedLiveChatId != null && cacheTimestamp != null
                && Instant.now().isBefore(cacheTimestamp.plus(CACHE_TTL))) {
            return cachedLiveChatId;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BROADCASTS_URL))
                    .header("Authorization", "Bearer " + config.getAccessToken())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new NotificationSendException("youtube",
                        "Failed to fetch active broadcast: " + response.statusCode() + ": " + response.body());
            }

            String body = response.body();
            String chatId = extractJsonString(body, "liveChatId");

            if (chatId == null) {
                throw new NotificationSendException("youtube",
                        "No active YouTube broadcast found. Start a live stream first or configure liveChatId.");
            }

            cachedLiveChatId = chatId;
            cacheTimestamp = Instant.now();
            log.debug("Fetched YouTube liveChatId: {}", chatId);
            return chatId;

        } catch (NotificationSendException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationSendException("youtube",
                    "Failed to fetch active broadcast: " + e.getMessage(), e);
        }
    }

    /** Clears cached liveChatId so next send fetches a fresh one. */
    public void clearCachedLiveChatId() {
        cachedLiveChatId = null;
        cacheTimestamp = null;
    }

    private String extractJsonString(String json, String key) {
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

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
