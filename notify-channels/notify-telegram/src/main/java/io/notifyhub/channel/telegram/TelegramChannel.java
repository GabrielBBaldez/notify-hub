package io.notifyhub.channel.telegram;

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
 * Telegram notification channel using the Bot API.
 *
 * <p>Sends messages via the Telegram Bot API ({@code /sendMessage}).
 * No external SDK needed — uses the JDK {@link HttpClient}.</p>
 *
 * <pre>{@code
 * TelegramChannel telegram = new TelegramChannel(
 *     TelegramConfig.builder()
 *         .botToken("123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11")
 *         .defaultChatId("123456789")
 *         .build()
 * );
 * }</pre>
 */
public class TelegramChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(TelegramChannel.class);
    private static final String TELEGRAM_API = "https://api.telegram.org/bot%s/sendMessage";
    private static final String TELEGRAM_PHOTO_API = "https://api.telegram.org/bot%s/sendPhoto";

    private final TelegramConfig config;
    private final HttpClient httpClient;

    public TelegramChannel(TelegramConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();
    }

    @Override
    public String getName() {
        return "telegram";
    }

    @Override
    public void send(Notification notification) {
        String content = notification.getRenderedContent();
        String chatId = resolveChatId(notification.getRecipient());

        if (chatId == null || chatId.isBlank()) {
            throw new NotificationSendException("telegram",
                    "No chat ID provided and no default chat ID configured");
        }

        String imageUrl = notification.getImageUrl();

        try {
            String payload;
            String url;

            if (imageUrl != null && !imageUrl.isBlank()) {
                payload = String.format(
                        "{\"chat_id\": \"%s\", \"photo\": \"%s\", \"caption\": \"%s\", \"parse_mode\": \"HTML\"}",
                        escapeJson(chatId),
                        escapeJson(imageUrl),
                        escapeJson(content)
                );
                url = String.format(TELEGRAM_PHOTO_API, config.getBotToken());
            } else {
                payload = String.format(
                        "{\"chat_id\": \"%s\", \"text\": \"%s\", \"parse_mode\": \"HTML\"}",
                        escapeJson(chatId),
                        escapeJson(content)
                );
                url = String.format(TELEGRAM_API, config.getBotToken());
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new NotificationSendException("telegram",
                        "Telegram API returned " + response.statusCode() + ": " + response.body());
            }

            log.debug("Telegram message sent to chat '{}': {}", chatId, response.body());

        } catch (NotificationSendException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationSendException("telegram",
                    "Failed to send Telegram message: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        return config.getBotToken() != null && !config.getBotToken().isBlank();
    }

    @Override
    public Map<String, String> getConfiguredRecipients() {
        return config.getRecipients();
    }

    private String resolveChatId(String recipient) {
        if (recipient != null && config.getRecipients().containsKey(recipient)) {
            return config.getRecipients().get(recipient);
        }
        if (recipient != null && !recipient.isBlank()) {
            return recipient;
        }
        return config.getDefaultChatId();
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
