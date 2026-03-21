package io.notifyhub.spring;

import io.notifyhub.spring.properties.*;
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
        private EmailProperties email;
        private SmsProperties sms;
        private WhatsAppProperties whatsapp;
        private SlackProperties slack;
        private TelegramProperties telegram;
        private DiscordProperties discord;
        private TeamsProperties teams;
        private PushProperties push;
        private List<WebhookEntryProperties> webhooks = new ArrayList<>();
        private WebSocketProperties websocket;
        private GoogleChatProperties googleChat;
        private TwitterProperties twitter;
        private LinkedInProperties linkedin;
        private NotionProperties notion;
        private TwitchProperties twitch;
        private YouTubeProperties youtube;
        private InstagramProperties instagram;
        private SendGridProperties sendgrid;
        private TikTokShopProperties tiktokShop;
        private FacebookProperties facebook;
        private AwsSnsProperties awsSns;
        private MailgunProperties mailgun;
        private PagerDutyProperties pagerduty;
        private KickProperties kick;

        public EmailProperties getEmail() { return email; }
        public void setEmail(EmailProperties email) { this.email = email; }
        public SmsProperties getSms() { return sms; }
        public void setSms(SmsProperties sms) { this.sms = sms; }
        public WhatsAppProperties getWhatsapp() { return whatsapp; }
        public void setWhatsapp(WhatsAppProperties whatsapp) { this.whatsapp = whatsapp; }
        public SlackProperties getSlack() { return slack; }
        public void setSlack(SlackProperties slack) { this.slack = slack; }
        public TelegramProperties getTelegram() { return telegram; }
        public void setTelegram(TelegramProperties telegram) { this.telegram = telegram; }
        public DiscordProperties getDiscord() { return discord; }
        public void setDiscord(DiscordProperties discord) { this.discord = discord; }
        public TeamsProperties getTeams() { return teams; }
        public void setTeams(TeamsProperties teams) { this.teams = teams; }
        public PushProperties getPush() { return push; }
        public void setPush(PushProperties push) { this.push = push; }
        public List<WebhookEntryProperties> getWebhooks() { return webhooks; }
        public void setWebhooks(List<WebhookEntryProperties> webhooks) { this.webhooks = webhooks; }
        public WebSocketProperties getWebsocket() { return websocket; }
        public void setWebsocket(WebSocketProperties websocket) { this.websocket = websocket; }
        public GoogleChatProperties getGoogleChat() { return googleChat; }
        public void setGoogleChat(GoogleChatProperties googleChat) { this.googleChat = googleChat; }
        public TwitterProperties getTwitter() { return twitter; }
        public void setTwitter(TwitterProperties twitter) { this.twitter = twitter; }
        public LinkedInProperties getLinkedin() { return linkedin; }
        public void setLinkedin(LinkedInProperties linkedin) { this.linkedin = linkedin; }
        public NotionProperties getNotion() { return notion; }
        public void setNotion(NotionProperties notion) { this.notion = notion; }
        public TwitchProperties getTwitch() { return twitch; }
        public void setTwitch(TwitchProperties twitch) { this.twitch = twitch; }
        public YouTubeProperties getYoutube() { return youtube; }
        public void setYoutube(YouTubeProperties youtube) { this.youtube = youtube; }
        public InstagramProperties getInstagram() { return instagram; }
        public void setInstagram(InstagramProperties instagram) { this.instagram = instagram; }
        public SendGridProperties getSendgrid() { return sendgrid; }
        public void setSendgrid(SendGridProperties sendgrid) { this.sendgrid = sendgrid; }
        public TikTokShopProperties getTiktokShop() { return tiktokShop; }
        public void setTiktokShop(TikTokShopProperties tiktokShop) { this.tiktokShop = tiktokShop; }
        public FacebookProperties getFacebook() { return facebook; }
        public void setFacebook(FacebookProperties facebook) { this.facebook = facebook; }
        public AwsSnsProperties getAwsSns() { return awsSns; }
        public void setAwsSns(AwsSnsProperties awsSns) { this.awsSns = awsSns; }
        public MailgunProperties getMailgun() { return mailgun; }
        public void setMailgun(MailgunProperties mailgun) { this.mailgun = mailgun; }
        public PagerDutyProperties getPagerduty() { return pagerduty; }
        public void setPagerduty(PagerDutyProperties pagerduty) { this.pagerduty = pagerduty; }
        public KickProperties getKick() { return kick; }
        public void setKick(KickProperties kick) { this.kick = kick; }
    }

    // --- Non-channel feature config inner classes (kept here) ---

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
