package io.notifyhub.channel.email;

/**
 * SMTP connection configuration.
 *
 * <pre>{@code
 * SmtpConfig config = SmtpConfig.builder()
 *     .host("smtp.gmail.com")
 *     .port(587)
 *     .username("user@gmail.com")
 *     .password("app-password")
 *     .from("noreply@myapp.com")
 *     .fromName("MyApp")
 *     .tls(true)
 *     .build();
 * }</pre>
 */
public class SmtpConfig {

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String from;
    private final String fromName;
    private final boolean tls;
    private final boolean ssl;
    private final int timeoutMs;

    private SmtpConfig(Builder builder) {
        this.host = requireNonBlank(builder.host, "SMTP host");
        this.port = builder.port;
        this.username = builder.username;
        this.password = builder.password;
        this.from = requireNonBlank(builder.from, "From address");
        this.fromName = builder.fromName;
        this.tls = builder.tls;
        this.ssl = builder.ssl;
        this.timeoutMs = builder.timeoutMs;
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFrom() { return from; }
    public String getFromName() { return fromName; }
    public boolean isTls() { return tls; }
    public boolean isSsl() { return ssl; }
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
        private String host;
        private int port = 587;
        private String username;
        private String password;
        private String from;
        private String fromName;
        private boolean tls = true;
        private boolean ssl = false;
        private int timeoutMs = 10_000;

        public Builder host(String host) { this.host = host; return this; }
        public Builder port(int port) { this.port = port; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder password(String password) { this.password = password; return this; }
        public Builder from(String from) { this.from = from; return this; }
        public Builder fromName(String fromName) { this.fromName = fromName; return this; }
        public Builder tls(boolean tls) { this.tls = tls; return this; }
        public Builder ssl(boolean ssl) { this.ssl = ssl; return this; }
        public Builder timeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; return this; }

        public SmtpConfig build() {
            return new SmtpConfig(this);
        }
    }
}
