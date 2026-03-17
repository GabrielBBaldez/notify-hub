package io.notifyhub.spring.properties;

import java.util.LinkedHashMap;
import java.util.Map;

public class TelegramProperties {
    private String botToken;
    private String chatId;
    private Map<String, String> recipients = new LinkedHashMap<>();
    public String getBotToken() { return botToken; }
    public void setBotToken(String botToken) { this.botToken = botToken; }
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }
    public Map<String, String> getRecipients() { return recipients; }
    public void setRecipients(Map<String, String> recipients) { this.recipients = recipients; }
}
