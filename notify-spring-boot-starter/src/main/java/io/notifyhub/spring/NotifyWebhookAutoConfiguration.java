package io.notifyhub.spring;

import io.notifyhub.channel.webhook.WebhookChannel;
import io.notifyhub.channel.webhook.WebhookConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Webhook auto-configuration — creates one {@link WebhookChannel} bean per
 * configured webhook entry.
 *
 * <p>Activated when notify-webhook module is on the classpath and at least
 * one webhook is configured via {@code notify.channels.webhooks[]}.</p>
 *
 * <pre>{@code
 * notify:
 *   channels:
 *     webhooks:
 *       - name: order-webhook
 *         url: https://api.example.com/notifications
 *         method: POST
 *       - name: audit-webhook
 *         url: https://audit.example.com/events
 *         headers:
 *           Authorization: "Bearer token123"
 * }</pre>
 */
@Configuration
@ConditionalOnClass(name = "io.notifyhub.channel.webhook.WebhookChannel")
public class NotifyWebhookAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NotifyWebhookAutoConfiguration.class);

    @Bean
    public List<WebhookChannel> webhookChannels(NotifyProperties properties) {
        List<NotifyProperties.WebhookEntry> entries = properties.getChannels().getWebhooks();
        List<WebhookChannel> channels = new ArrayList<>();

        if (entries == null || entries.isEmpty()) {
            return channels;
        }

        for (NotifyProperties.WebhookEntry entry : entries) {
            if (entry.getUrl() == null || entry.getUrl().isBlank()) {
                log.warn("NotifyHub: Skipping webhook '{}' — no URL configured", entry.getName());
                continue;
            }

            WebhookConfig.Builder configBuilder = WebhookConfig.builder()
                    .name(entry.getName() != null ? entry.getName() : "webhook")
                    .url(entry.getUrl());

            if (entry.getPayloadTemplate() != null && !entry.getPayloadTemplate().isBlank()) {
                configBuilder.payloadTemplate(entry.getPayloadTemplate());
            }
            if (entry.getHeaders() != null && !entry.getHeaders().isEmpty()) {
                configBuilder.headers(entry.getHeaders());
            }
            if (entry.getMethod() != null && !entry.getMethod().isBlank()) {
                configBuilder.method(entry.getMethod());
            }

            WebhookChannel channel = new WebhookChannel(configBuilder.build());
            channels.add(channel);
            log.info("NotifyHub: Webhook channel '{}' configured ({})", entry.getName(), entry.getUrl());
        }

        return channels;
    }
}
