package io.notifyhub.demo;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Starts an embedded SMTP server (GreenMail) on port 3025.
 * This way the demo works without any external SMTP configuration.
 *
 * Emails sent by NotifyHub are received here and can be
 * read via the /inbox endpoint.
 */
@Configuration
@ConditionalOnProperty(name = "demo.embedded-smtp", havingValue = "true", matchIfMissing = true)
public class EmbeddedSmtpConfig {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedSmtpConfig.class);

    private GreenMail greenMail;

    @PostConstruct
    public void start() {
        greenMail = new GreenMail(new ServerSetup(3025, "localhost", "smtp"));
        greenMail.start();
        log.info("===========================================");
        log.info("  Embedded SMTP server started on port 3025");
        log.info("  All emails will be captured here");
        log.info("  Check inbox at: GET /inbox");
        log.info("===========================================");
    }

    @PreDestroy
    public void stop() {
        if (greenMail != null) {
            greenMail.stop();
            log.info("Embedded SMTP server stopped");
        }
    }

    public GreenMail getGreenMail() {
        return greenMail;
    }
}
