package io.notifyhub.channel.telegram;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Telegram Bot API configuration.
 *
 * <pre>{@code
 * TelegramConfig config = TelegramConfig.builder()
 *     .botToken("123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11")
 *     .defaultChatId("123456789")
 *     .build();
 * }</pre>
 */
public class TelegramConfig {

    private final String botToken;
    private final String defaultChatId;
    private final Map<String, String> recipients;
    private final int timeoutMs;

    private TelegramConfig(Builder builder) {
        this.botToken = requireNonBlank(builder.botToken, "Telegram bot token");
        this.defaultChatId = builder.defaultChatId;
        this.recipients = builder.recipients != null ? Collections.unmodifiableMap(new LinkedHashMap<>(builder.recipients)) : Collections.emptyMap();
        this.timeoutMs = builder.timeoutMs;
    }

    public String getBotToken() { return botToken; }
    public String getDefaultChatId() { return defaultChatId; }
    public Map<String, String> getRecipients() { return recipients; }
    public int getTimeoutMs() { return timeoutMs; }

    public static Builder builder() {
        return new Builder();
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be null or blank");
        }
        return value;
    }

    public static class Builder {
        private String botToken;
        private String defaultChatId;
        private Map<String, String> recipients;
        private int timeoutMs = 10_000;

        public Builder botToken(String botToken) { this.botToken = botToken; return this; }
        public Builder defaultChatId(String defaultChatId) { this.defaultChatId = defaultChatId; return this; }
        public Builder recipients(Map<String, String> recipients) { this.recipients = recipients; return this; }
        public Builder timeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; return this; }

        public TelegramConfig build() {
            return new TelegramConfig(this);
        }
    }
}
