package io.notifyhub.spring.properties;

public class EmailProperties {
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
