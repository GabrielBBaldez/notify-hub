package io.notifyhub.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "notify")
public class NotifyProperties {

    private Channels channels = new Channels();
    private Retry retry = new Retry();
    private Scheduling scheduling = new Scheduling();
    private Tracking tracking = new Tracking();
    private Events events = new Events();
    private RateLimit rateLimit = new RateLimit();
    private Deduplication deduplication = new Deduplication();
    private Audit audit = new Audit();
    private StatusWebhook statusWebhook = new StatusWebhook();
    private Audience audience = new Audience();

    public Channels getChannels() { return channels; }
    public void setChannels(Channels channels) { this.channels = channels; }
    public Retry getRetry() { return retry; }
    public void setRetry(Retry retry) { this.retry = retry; }
    public Scheduling getScheduling() { return scheduling; }
    public void setScheduling(Scheduling scheduling) { this.scheduling = scheduling; }
    public Tracking getTracking() { return tracking; }
    public void setTracking(Tracking tracking) { this.tracking = tracking; }
    public Events getEvents() { return events; }
    public void setEvents(Events events) { this.events = events; }
    public RateLimit getRateLimit() { return rateLimit; }
    public void setRateLimit(RateLimit rateLimit) { this.rateLimit = rateLimit; }
    public Deduplication getDeduplication() { return deduplication; }
    public void setDeduplication(Deduplication deduplication) { this.deduplication = deduplication; }
    public Audit getAudit() { return audit; }
    public void setAudit(Audit audit) { this.audit = audit; }
    public StatusWebhook getStatusWebhook() { return statusWebhook; }
    public void setStatusWebhook(StatusWebhook statusWebhook) { this.statusWebhook = statusWebhook; }
    public Audience getAudience() { return audience; }
    public void setAudience(Audience audience) { this.audience = audience; }

    public static class Channels {
        private Email email;
        private Sms sms;
        private WhatsApp whatsapp;
        private Slack slack;
        private Telegram telegram;
        private Discord discord;
        private Teams teams;
        private Push push;
        private List<WebhookEntry> webhooks = new ArrayList<>();
        private WebSocket websocket;
        private GoogleChat googleChat;
        private Twitter twitter;
        private LinkedIn linkedin;
        private Notion notion;
        private Twitch twitch;
        private YouTube youtube;
        private Instagram instagram;

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
        public Teams getTeams() { return teams; }
        public void setTeams(Teams teams) { this.teams = teams; }
        public Push getPush() { return push; }
        public void setPush(Push push) { this.push = push; }
        public List<WebhookEntry> getWebhooks() { return webhooks; }
        public void setWebhooks(List<WebhookEntry> webhooks) { this.webhooks = webhooks; }
        public WebSocket getWebsocket() { return websocket; }
        public void setWebsocket(WebSocket websocket) { this.websocket = websocket; }
        public GoogleChat getGoogleChat() { return googleChat; }
        public void setGoogleChat(GoogleChat googleChat) { this.googleChat = googleChat; }
        public Twitter getTwitter() { return twitter; }
        public void setTwitter(Twitter twitter) { this.twitter = twitter; }
        public LinkedIn getLinkedin() { return linkedin; }
        public void setLinkedin(LinkedIn linkedin) { this.linkedin = linkedin; }
        public Notion getNotion() { return notion; }
        public void setNotion(Notion notion) { this.notion = notion; }
        public Twitch getTwitch() { return twitch; }
        public void setTwitch(Twitch twitch) { this.twitch = twitch; }
        public YouTube getYoutube() { return youtube; }
        public void setYoutube(YouTube youtube) { this.youtube = youtube; }
        public Instagram getInstagram() { return instagram; }
        public void setInstagram(Instagram instagram) { this.instagram = instagram; }
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
        private Map<String, String> recipients = new LinkedHashMap<>();
        public String getWebhookUrl() { return webhookUrl; }
        public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
        public Map<String, String> getRecipients() { return recipients; }
        public void setRecipients(Map<String, String> recipients) { this.recipients = recipients; }
    }

    public static class Telegram {
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

    public static class Discord {
        private String webhookUrl;
        private String username;
        private String avatarUrl;
        private Map<String, String> recipients = new LinkedHashMap<>();
        public String getWebhookUrl() { return webhookUrl; }
        public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
        public Map<String, String> getRecipients() { return recipients; }
        public void setRecipients(Map<String, String> recipients) { this.recipients = recipients; }
    }

    public static class Teams {
        private String webhookUrl;
        private Map<String, String> recipients = new LinkedHashMap<>();
        public String getWebhookUrl() { return webhookUrl; }
        public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
        public Map<String, String> getRecipients() { return recipients; }
        public void setRecipients(Map<String, String> recipients) { this.recipients = recipients; }
    }

    public static class Push {
        private String serverKey;
        public String getServerKey() { return serverKey; }
        public void setServerKey(String serverKey) { this.serverKey = serverKey; }
    }

    public static class WebSocket {
        private String uri;
        private int timeoutMs = 10_000;
        private boolean reconnectEnabled = true;
        private long reconnectDelayMs = 5_000;
        private int maxReconnectAttempts = 3;
        private Map<String, String> headers = new LinkedHashMap<>();
        private String messageFormat = "{{content}}";
        public String getUri() { return uri; }
        public void setUri(String uri) { this.uri = uri; }
        public int getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
        public boolean isReconnectEnabled() { return reconnectEnabled; }
        public void setReconnectEnabled(boolean reconnectEnabled) { this.reconnectEnabled = reconnectEnabled; }
        public long getReconnectDelayMs() { return reconnectDelayMs; }
        public void setReconnectDelayMs(long reconnectDelayMs) { this.reconnectDelayMs = reconnectDelayMs; }
        public int getMaxReconnectAttempts() { return maxReconnectAttempts; }
        public void setMaxReconnectAttempts(int maxReconnectAttempts) { this.maxReconnectAttempts = maxReconnectAttempts; }
        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
        public String getMessageFormat() { return messageFormat; }
        public void setMessageFormat(String messageFormat) { this.messageFormat = messageFormat; }
    }

    public static class GoogleChat {
        private String webhookUrl;
        private int timeoutMs = 10_000;
        private Map<String, String> recipients = new LinkedHashMap<>();
        public String getWebhookUrl() { return webhookUrl; }
        public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
        public int getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
        public Map<String, String> getRecipients() { return recipients; }
        public void setRecipients(Map<String, String> recipients) { this.recipients = recipients; }
    }

    public static class Twitter {
        private String apiKey;
        private String apiSecret;
        private String accessToken;
        private String accessTokenSecret;
        private Map<String, String> recipients = new LinkedHashMap<>();
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getApiSecret() { return apiSecret; }
        public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public String getAccessTokenSecret() { return accessTokenSecret; }
        public void setAccessTokenSecret(String accessTokenSecret) { this.accessTokenSecret = accessTokenSecret; }
        public Map<String, String> getRecipients() { return recipients; }
        public void setRecipients(Map<String, String> recipients) { this.recipients = recipients; }
    }

    public static class LinkedIn {
        private String accessToken;
        private String authorId;
        private Map<String, String> recipients = new LinkedHashMap<>();
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public String getAuthorId() { return authorId; }
        public void setAuthorId(String authorId) { this.authorId = authorId; }
        public Map<String, String> getRecipients() { return recipients; }
        public void setRecipients(Map<String, String> recipients) { this.recipients = recipients; }
    }

    public static class Notion {
        private String apiKey;
        private String databaseId;
        private Map<String, String> recipients = new LinkedHashMap<>();
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getDatabaseId() { return databaseId; }
        public void setDatabaseId(String databaseId) { this.databaseId = databaseId; }
        public Map<String, String> getRecipients() { return recipients; }
        public void setRecipients(Map<String, String> recipients) { this.recipients = recipients; }
    }

    public static class Twitch {
        private String clientId;
        private String accessToken;
        private String refreshToken;
        private String clientSecret;
        private String broadcasterId;
        private String senderId;
        private Map<String, String> recipients = new LinkedHashMap<>();
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        public String getBroadcasterId() { return broadcasterId; }
        public void setBroadcasterId(String broadcasterId) { this.broadcasterId = broadcasterId; }
        public String getSenderId() { return senderId; }
        public void setSenderId(String senderId) { this.senderId = senderId; }
        public Map<String, String> getRecipients() { return recipients; }
        public void setRecipients(Map<String, String> recipients) { this.recipients = recipients; }
    }

    public static class YouTube {
        private String accessToken;
        private String refreshToken;
        private String clientId;
        private String clientSecret;
        private String channelId;
        private String liveChatId;
        private Map<String, String> recipients = new LinkedHashMap<>();
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        public String getChannelId() { return channelId; }
        public void setChannelId(String channelId) { this.channelId = channelId; }
        public String getLiveChatId() { return liveChatId; }
        public void setLiveChatId(String liveChatId) { this.liveChatId = liveChatId; }
        public Map<String, String> getRecipients() { return recipients; }
        public void setRecipients(Map<String, String> recipients) { this.recipients = recipients; }
    }

    public static class Instagram {
        private String accessToken;
        private String igUserId;
        private Map<String, String> recipients = new LinkedHashMap<>();
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public String getIgUserId() { return igUserId; }
        public void setIgUserId(String igUserId) { this.igUserId = igUserId; }
        public Map<String, String> getRecipients() { return recipients; }
        public void setRecipients(Map<String, String> recipients) { this.recipients = recipients; }
    }

    public static class WebhookEntry {
        private String name;
        private String url;
        private String payloadTemplate;
        private Map<String, String> headers = new LinkedHashMap<>();
        private String method = "POST";
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getPayloadTemplate() { return payloadTemplate; }
        public void setPayloadTemplate(String payloadTemplate) { this.payloadTemplate = payloadTemplate; }
        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
    }

    public static class Retry {
        private int maxAttempts = 1;
        private String strategy = "none";
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
        private String type = "memory";
        private boolean dlqEnabled = false;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public boolean isDlqEnabled() { return dlqEnabled; }
        public void setDlqEnabled(boolean dlqEnabled) { this.dlqEnabled = dlqEnabled; }
    }

    public static class Events {
        private boolean enabled = true;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class RateLimit {
        private boolean enabled = false;
        private boolean useDefaults = true;
        private int maxRequests = 100;
        private String window = "60s";
        private Map<String, ChannelRateLimit> channels = new LinkedHashMap<>();
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isUseDefaults() { return useDefaults; }
        public void setUseDefaults(boolean useDefaults) { this.useDefaults = useDefaults; }
        public int getMaxRequests() { return maxRequests; }
        public void setMaxRequests(int maxRequests) { this.maxRequests = maxRequests; }
        public String getWindow() { return window; }
        public void setWindow(String window) { this.window = window; }
        public Map<String, ChannelRateLimit> getChannels() { return channels; }
        public void setChannels(Map<String, ChannelRateLimit> channels) { this.channels = channels; }
    }

    public static class Deduplication {
        private boolean enabled = false;
        private String ttl = "24h";
        private String strategy = "content-hash";
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getTtl() { return ttl; }
        public void setTtl(String ttl) { this.ttl = ttl; }
        public String getStrategy() { return strategy; }
        public void setStrategy(String strategy) { this.strategy = strategy; }
    }

    public static class ChannelRateLimit {
        private int maxRequests;
        private String window;
        public int getMaxRequests() { return maxRequests; }
        public void setMaxRequests(int maxRequests) { this.maxRequests = maxRequests; }
        public String getWindow() { return window; }
        public void setWindow(String window) { this.window = window; }
    }

    public static class Audit {
        private boolean enabled = false;
        private String type = "memory";
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    public static class StatusWebhook {
        private String url;
        private int timeoutMs = 10_000;
        private Map<String, String> headers = new LinkedHashMap<>();
        private String signingSecret;
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public int getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
        public String getSigningSecret() { return signingSecret; }
        public void setSigningSecret(String signingSecret) { this.signingSecret = signingSecret; }
    }

    public static class Audience {
        private boolean enabled = false;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
