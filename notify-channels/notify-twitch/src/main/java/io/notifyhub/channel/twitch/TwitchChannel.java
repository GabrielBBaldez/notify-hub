package io.notifyhub.channel.twitch;

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
import java.util.Map;

/**
 * Twitch notification channel — sends chat messages and creates polls via Helix API.
 *
 * <p>Posts messages to a Twitch channel chat using
 * {@code POST https://api.twitch.tv/helix/chat/messages}.</p>
 *
 * <p>Creates polls when {@code poll_choices} param is present using
 * {@code POST https://api.twitch.tv/helix/polls}.
 * Requires scope {@code channel:manage:polls} (affiliate/partner only).</p>
 */
public class TwitchChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(TwitchChannel.class);
    private static final String CHAT_URL = "https://api.twitch.tv/helix/chat/messages";
    private static final String POLLS_URL = "https://api.twitch.tv/helix/polls";
    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final int DEFAULT_POLL_DURATION = 60;

    private final TwitchConfig config;
    private final HttpClient httpClient;

    public TwitchChannel(TwitchConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();
    }

    @Override
    public String getName() {
        return "twitch";
    }

    @Override
    public void send(Notification notification) {
        Map<String, Object> params = notification.getParams();
        String pollChoices = params != null ? (String) params.get("poll_choices") : null;

        if (pollChoices != null && !pollChoices.isBlank()) {
            sendPoll(notification, pollChoices);
        } else {
            sendChatMessage(notification);
        }
    }

    private void sendChatMessage(Notification notification) {
        String content = notification.getRenderedContent();
        String broadcasterId = resolveBroadcasterId(notification.getRecipient());

        if (content.length() > MAX_MESSAGE_LENGTH) {
            content = content.substring(0, MAX_MESSAGE_LENGTH - 3) + "...";
        }

        try {
            String payload = "{" +
                    "\"broadcaster_id\": \"" + escapeJson(broadcasterId) + "\", " +
                    "\"sender_id\": \"" + escapeJson(config.getSenderId()) + "\", " +
                    "\"message\": \"" + escapeJson(content) + "\"" +
                    "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CHAT_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getAccessToken())
                    .header("Client-Id", config.getClientId())
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new NotificationSendException("twitch",
                        "Twitch API returned " + response.statusCode() + ": " + response.body());
            }

            log.debug("Twitch chat message sent to broadcaster {}: {}", broadcasterId, response.statusCode());

        } catch (NotificationSendException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationSendException("twitch",
                    "Failed to send Twitch chat message: " + e.getMessage(), e);
        }
    }

    private void sendPoll(Notification notification, String pollChoices) {
        String broadcasterId = resolveBroadcasterId(notification.getRecipient());
        String title = notification.getRenderedContent();
        Map<String, Object> params = notification.getParams();

        int duration = DEFAULT_POLL_DURATION;
        Object durationParam = params.get("poll_duration");
        if (durationParam != null) {
            duration = Integer.parseInt(durationParam.toString());
        }

        // Build choices JSON array
        String[] choices = pollChoices.split(",");
        StringBuilder choicesJson = new StringBuilder("[");
        for (int i = 0; i < choices.length; i++) {
            if (i > 0) choicesJson.append(", ");
            choicesJson.append("{\"title\": \"").append(escapeJson(choices[i].trim())).append("\"}");
        }
        choicesJson.append("]");

        try {
            String payload = "{" +
                    "\"broadcaster_id\": \"" + escapeJson(broadcasterId) + "\", " +
                    "\"title\": \"" + escapeJson(title) + "\", " +
                    "\"choices\": " + choicesJson + ", " +
                    "\"duration\": " + duration +
                    "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(POLLS_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getAccessToken())
                    .header("Client-Id", config.getClientId())
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new NotificationSendException("twitch",
                        "Twitch Polls API returned " + response.statusCode() + ": " + response.body());
            }

            log.debug("Twitch poll created for broadcaster {}: {}", broadcasterId, title);

        } catch (NotificationSendException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationSendException("twitch",
                    "Failed to create Twitch poll: " + e.getMessage(), e);
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

    private String resolveBroadcasterId(String recipient) {
        if (recipient != null && config.getRecipients().containsKey(recipient)) {
            return config.getRecipients().get(recipient);
        }
        if (recipient != null && !recipient.isBlank() && !"default".equals(recipient) && recipient.matches("\\d+")) {
            return recipient;
        }
        return config.getBroadcasterId();
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
