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
 *   retry:
 *     max-attempts: 3
 *     strategy: exponential
 * }</pre>
 */
@ConfigurationProperties(prefix = "notify")
public class NotifyProperties {

    private Channels channels = new Channels();
    private Retry retry = new Retry();

    public Channels getChannels() { return channels; }
    public void setChannels(Channels channels) { this.channels = channels; }
    public Retry getRetry() { return retry; }
    public void setRetry(Retry retry) { this.retry = retry; }

    public static class Channels {

        private Email email;
        private Sms sms;
        private WhatsApp whatsapp;

        public Email getEmail() { return email; }
        public void setEmail(Email email) { this.email = email; }
        public Sms getSms() { return sms; }
        public void setSms(Sms sms) { this.sms = sms; }
        public WhatsApp getWhatsapp() { return whatsapp; }
        public void setWhatsapp(WhatsApp whatsapp) { this.whatsapp = whatsapp; }
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

    public static class Retry {
        private int maxAttempts = 1;
        private String strategy = "none"; // none, fixed, exponential

        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        public String getStrategy() { return strategy; }
        public void setStrategy(String strategy) { this.strategy = strategy; }
    }
}
