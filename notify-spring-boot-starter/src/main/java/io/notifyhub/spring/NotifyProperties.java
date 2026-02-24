package io.notifyhub.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for NotifyHub.
 * Bind from {@code application.yml} under the {@code notify} prefix.
 *
 * <pre>{@code
 * notify:
 *   channels:
 *     email:
 *       host: smtp.gmail.com
 *       port: 587
 *       username: ${MAIL_USER}
 *       password: ${MAIL_PASS}
 *       from: noreply@myapp.com
 *       from-name: MyApp
 *       tls: true
 *     sms:
 *       account-sid: ${TWILIO_SID}
 *       auth-token: ${TWILIO_TOKEN}
 *       from-number: "+5548999999999"
 *     whatsapp:
 *       account-sid: ${TWILIO_SID}
 *       auth-token: ${TWILIO_TOKEN}
 *       from-number: "+14155238886"
 *     slack:
 *       webhook-url: https://hooks.slack.com/services/XXX/YYY/ZZZ
 *     telegram:
 *       bot-token: "123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11"
 *       chat-id: "123456789"
 *     discord:
 *       webhook-url: https://discord.com/api/webhooks/123/abc
 *       username: NotifyHub Bot
 *   retry:
 *     max-attempts: 3
 *     strategy: exponential
 * }</pre>
 */
@ConfigurationProperties(prefix = "notify")
public class NotifyProperties {

    private Channels channels = new Channels();
    private Retry retry = new Retry();
    private Scheduling scheduling = new Scheduling();
    private Tracking tracking = new Tracking();

    public Channels getChannels() { return channels; }
    public void setChannels(Channels channels) { this.channels = channels; }
    public Retry getRetry() { return retry; }
    public void setRetry(Retry retry) { this.retry = retry; }
    public Scheduling getScheduling() { return scheduling; }
    public void setScheduling(Scheduling scheduling) { this.scheduling = scheduling; }
    public Tracking getTracking() { return tracking; }
    public void setTracking(Tracking tracking) { this.tracking = tracking; }

    public static class Channels {

        private Email email;
        private Sms sms;
        private WhatsApp whatsapp;
        private Slack slack;
        private Telegram telegram;
        private Discord discord;

        public Email getEmail() { return email; }
        public void setEmail(Email email) { this.email = email; }
        public Sms getSms() { return sms; }
        public void setSms(Sms sms) { this.sms = sms; }
        public WhatsApp getWhatsapp() { return whatsapp; }
        public void setWhatsapp(WhatsApp whatsapp) { this.whatsapp = whatsapp; }
        public Slack getSlack() { return slack; }
        public void setSlack(Slack slack) { this.slack = slack; }
        public Telegram getTelegram() { return telegram; }
        public void setTelegram(Telegram telegram) { this.telegram = telegram; }
        public Discord getDiscord() { return discord; }
        public void setDiscord(Discord discord) { this.discord = discord; }
    }

    public static class Email {
        private String host;
        private int port = 587;
        private String username;
        private String password;
        private String from;
        private String fromName;
        private boolean tls = true;
        private boolean ssl = false;

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        public String getFromName() { return fromName; }
        public void setFromName(String fromName) { this.fromName = fromName; }
        public boolean isTls() { return tls; }
        public void setTls(boolean tls) { this.tls = tls; }
        public boolean isSsl() { return ssl; }
        public void setSsl(boolean ssl) { this.ssl = ssl; }
    }

    public static class Sms {
        private String accountSid;
        private String authToken;
        private String fromNumber;

        public String getAccountSid() { return accountSid; }
        public void setAccountSid(String accountSid) { this.accountSid = accountSid; }
        public String getAuthToken() { return authToken; }
        public void setAuthToken(String authToken) { this.authToken = authToken; }
        public String getFromNumber() { return fromNumber; }
        public void setFromNumber(String fromNumber) { this.fromNumber = fromNumber; }
    }

    public static class WhatsApp {
        private String accountSid;
        private String authToken;
        private String fromNumber;

        public String getAccountSid() { return accountSid; }
        public void setAccountSid(String accountSid) { this.accountSid = accountSid; }
        public String getAuthToken() { return authToken; }
        public void setAuthToken(String authToken) { this.authToken = authToken; }
        public String getFromNumber() { return fromNumber; }
        public void setFromNumber(String fromNumber) { this.fromNumber = fromNumber; }
    }

    public static class Slack {
        private String webhookUrl;

        public String getWebhookUrl() { return webhookUrl; }
        public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
    }

    public static class Telegram {
        private String botToken;
        private String chatId;

        public String getBotToken() { return botToken; }
        public void setBotToken(String botToken) { this.botToken = botToken; }
        public String getChatId() { return chatId; }
        public void setChatId(String chatId) { this.chatId = chatId; }
    }

    public static class Discord {
        private String webhookUrl;
        private String username;
        private String avatarUrl;

        public String getWebhookUrl() { return webhookUrl; }
        public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    }

    public static class Retry {
        private int maxAttempts = 1;
        private String strategy = "none"; // none, fixed, exponential

        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        public String getStrategy() { return strategy; }
        public void setStrategy(String strategy) { this.strategy = strategy; }
    }

    public static class Scheduling {
        private boolean enabled = true;
        private int poolSize = 2;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getPoolSize() { return poolSize; }
        public void setPoolSize(int poolSize) { this.poolSize = poolSize; }
    }

    public static class Tracking {
        private boolean enabled = false;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
