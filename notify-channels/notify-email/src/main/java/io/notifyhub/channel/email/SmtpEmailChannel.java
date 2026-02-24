package io.notifyhub.channel.email;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Email notification channel using SMTP (Jakarta Mail).
 *
 * <pre>{@code
 * SmtpConfig config = SmtpConfig.builder()
 *     .host("smtp.gmail.com")
 *     .port(587)
 *     .username("user@gmail.com")
 *     .password("app-password")
 *     .from("noreply@myapp.com")
 *     .tls(true)
 *     .build();
 *
 * SmtpEmailChannel emailChannel = new SmtpEmailChannel(config);
 * }</pre>
 */
public class SmtpEmailChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailChannel.class);

    private final SmtpConfig config;
    private final Session session;

    public SmtpEmailChannel(SmtpConfig config) {
        this.config = config;
        this.session = createSession();
    }

    @Override
    public String getName() {
        return "email";
    }

    @Override
    public void send(Notification notification) {
        try {
            MimeMessage message = new MimeMessage(session);

            // From
            if (config.getFromName() != null) {
                message.setFrom(new InternetAddress(config.getFrom(), config.getFromName()));
            } else {
                message.setFrom(new InternetAddress(config.getFrom()));
            }

            // To
            message.setRecipient(Message.RecipientType.TO,
                    new InternetAddress(notification.getRecipient()));

            // Subject
            String subject = notification.getSubject() != null
                    ? notification.getSubject()
                    : "Notification";
            message.setSubject(subject, "UTF-8");

            // Body
            String content = notification.getRenderedContent();
            if (isHtml(content)) {
                message.setContent(content, "text/html; charset=UTF-8");
            } else {
                message.setText(content, "UTF-8");
            }

            // Send
            Transport.send(message);
            log.debug("Email sent to '{}' with subject '{}'",
                    notification.getRecipient(), subject);

        } catch (Exception e) {
            throw new NotificationSendException("email",
                    "Failed to send email to '" + notification.getRecipient() + "': " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        try (Transport transport = session.getTransport("smtp")) {
            transport.connect(config.getHost(), config.getPort(),
                    config.getUsername(), config.getPassword());
            return true;
        } catch (Exception e) {
            log.warn("SMTP health check failed: {}", e.getMessage());
            return false;
        }
    }

    private Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", config.getHost());
        props.put("mail.smtp.port", String.valueOf(config.getPort()));
        props.put("mail.smtp.auth", String.valueOf(config.getUsername() != null));
        props.put("mail.smtp.connectiontimeout", String.valueOf(config.getTimeoutMs()));
        props.put("mail.smtp.timeout", String.valueOf(config.getTimeoutMs()));

        if (config.isTls()) {
            props.put("mail.smtp.starttls.enable", "true");
        }
        if (config.isSsl()) {
            props.put("mail.smtp.ssl.enable", "true");
        }

        if (config.getUsername() != null) {
            return Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(config.getUsername(), config.getPassword());
                }
            });
        }

        return Session.getInstance(props);
    }

    private boolean isHtml(String content) {
        return content != null && (
                content.contains("<html") ||
                content.contains("<body") ||
                content.contains("<div") ||
                content.contains("<p>") ||
                content.contains("<h1")
        );
    }
}
