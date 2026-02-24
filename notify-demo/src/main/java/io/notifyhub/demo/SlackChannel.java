package io.notifyhub.demo;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Slack notification channel using Incoming Webhooks.
 *
 * <p>When {@code demo.slack.webhook-url} is configured, sends real messages
 * to Slack via webhook. Otherwise, stores messages in-memory (fake mode).</p>
 */
@Component
public class SlackChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(SlackChannel.class);

    private final List<String> messages = Collections.synchronizedList(new ArrayList<>());
    private final String webhookUrl;
    private final HttpClient httpClient;

    public SlackChannel(
            @Value("${demo.slack.webhook-url:}") String webhookUrl) {
        this.webhookUrl = webhookUrl;
        this.httpClient = HttpClient.newHttpClient();

        if (isRealMode()) {
            log.info("Slack channel configured with real webhook");
        } else {
            log.info("Slack channel running in fake mode (no webhook URL)");
        }
    }

    @Override
    public String getName() {
        return "slack";
    }

    @Override
    public void send(Notification notification) {
        String content = notification.getRenderedContent();
        String recipient = notification.getRecipient();

        if (isRealMode()) {
            sendToWebhook(content, recipient);
        }

        // Always store locally (for /inbox/slack endpoint)
        String msg = String.format("[Slack → %s] %s", recipient, content);
        messages.add(msg);
        log.info("Slack message sent: {}", msg);
    }

    private void sendToWebhook(String content, String channel) {
        try {
            // Slack webhook payload
            String payload = String.format(
                    "{\"text\": \"%s\", \"channel\": \"%s\"}",
                    escapeJson(content),
                    escapeJson(channel)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new NotificationSendException("slack",
                        "Slack webhook returned " + response.statusCode() + ": " + response.body());
            }

            log.debug("Slack webhook response: {}", response.body());

        } catch (NotificationSendException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationSendException("slack",
                    "Failed to send Slack webhook: " + e.getMessage(), e);
        }
    }

    private boolean isRealMode() {
        return webhookUrl != null && !webhookUrl.isBlank();
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n");
    }

    public List<String> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public void clear() {
        messages.clear();
    }
}
