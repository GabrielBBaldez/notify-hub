package io.notifyhub.queue.kafka;

import io.notifyhub.core.Channel;
import io.notifyhub.core.NotificationBuilder;
import io.notifyhub.core.NotifyHub;
import io.notifyhub.core.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

/**
 * Consumes notifications from a Kafka topic and sends them via NotifyHub.
 * Automatically activated when {@code notify.queue.kafka.consumer.enabled=true}.
 */
public class KafkaNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaNotificationConsumer.class);

    private final NotifyHub hub;

    public KafkaNotificationConsumer(NotifyHub hub) {
        this.hub = hub;
    }

    @KafkaListener(topics = "${notify.queue.kafka.topic:notifyhub-notifications}", groupId = "${notify.queue.kafka.consumer.group-id:notifyhub-group}")
    public void onMessage(QueuedNotification notification) {
        log.info("Consuming notification from Kafka: channel={}, recipient={}",
                notification.getChannelName(), notification.getRecipient());
        try {
            NotificationBuilder builder = resolveRecipient(notification);

            builder.via(Channel.custom(notification.getChannelName()));

            if (notification.getSubject() != null) {
                builder.subject(notification.getSubject());
            }

            if (notification.getTemplateName() != null) {
                builder.template(notification.getTemplateName());
                if (notification.getParams() != null && !notification.getParams().isEmpty()) {
                    builder.params(notification.getParams());
                }
            } else {
                builder.content(notification.getRawContent());
            }

            if (notification.getPriority() != null) {
                builder.priority(Priority.valueOf(notification.getPriority()));
            }

            if (notification.getDeduplicationKey() != null) {
                builder.deduplicationKey(notification.getDeduplicationKey());
            }

            builder.sendTracked();
            log.info("Notification sent successfully: channel={}, recipient={}",
                    notification.getChannelName(), notification.getRecipient());
        } catch (Exception e) {
            log.error("Failed to send notification: channel={}, recipient={}, error={}",
                    notification.getChannelName(), notification.getRecipient(), e.getMessage(), e);
            throw e;
        }
    }

    private NotificationBuilder resolveRecipient(QueuedNotification notification) {
        String channel = notification.getChannelName().toLowerCase();
        String recipient = notification.getRecipient();
        if ("sms".equals(channel) || "whatsapp".equals(channel)) {
            return hub.toPhone(recipient);
        }
        return hub.to(recipient);
    }
}
