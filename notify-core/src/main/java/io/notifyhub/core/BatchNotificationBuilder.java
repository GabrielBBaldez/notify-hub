package io.notifyhub.core;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Builder for sending the same notification to multiple recipients (batch send).
 *
 * <pre>{@code
 * // Batch send to email addresses
 * List<DeliveryReceipt> receipts = notify.toAll(List.of("a@test.com", "b@test.com"))
 *     .via(Channel.EMAIL)
 *     .template("announcement")
 *     .param("message", "Hello everyone!")
 *     .send();
 *
 * // Batch send to Notifiable entities
 * List<DeliveryReceipt> receipts = notify.toAllNotifiable(users)
 *     .via(Channel.EMAIL)
 *     .template("weekly-digest")
 *     .send();
 * }</pre>
 */
public class BatchNotificationBuilder {

    private final NotifyHub hub;
    private final List<String> recipients;
    private final List<? extends Notifiable> notifiables;

    // Fluent fields (same as NotificationBuilder)
    private final List<String> channels = new ArrayList<>();
    private String subject;
    private String templateName;
    private String rawContent;
    private final Map<String, Object> params = new LinkedHashMap<>();
    private Priority priority = Priority.NORMAL;
    private String imageUrl;

    // Package-private constructor
    BatchNotificationBuilder(NotifyHub hub, List<String> recipients, List<? extends Notifiable> notifiables) {
        this.hub = hub;
        this.recipients = recipients;
        this.notifiables = notifiables;
    }

    // ===================== CHANNELS =====================

    /** Set the primary channel(s) to send through. */
    public BatchNotificationBuilder via(Channel... channels) {
        for (Channel ch : channels) {
            this.channels.add(ch.name().toLowerCase().replace('_', '-'));
        }
        return this;
    }

    /** Set a custom channel by reference. */
    public BatchNotificationBuilder via(ChannelRef ref) {
        this.channels.add(ref.getName());
        return this;
    }

    // ===================== CONTENT =====================

    /** Set the email subject line. */
    public BatchNotificationBuilder subject(String subject) {
        this.subject = subject;
        return this;
    }

    /** Set the template name (loaded from resources/templates/notify/). */
    public BatchNotificationBuilder template(String templateName) {
        this.templateName = templateName;
        return this;
    }

    /** Set raw content directly (no template). */
    public BatchNotificationBuilder content(String content) {
        this.rawContent = content;
        return this;
    }

    /** Add a template parameter. */
    public BatchNotificationBuilder param(String key, Object value) {
        this.params.put(key, value);
        return this;
    }

    /** Add multiple template parameters. */
    public BatchNotificationBuilder params(Map<String, Object> params) {
        this.params.putAll(params);
        return this;
    }

    // ===================== IMAGE =====================

    /** Set an image URL to embed in the notification. */
    public BatchNotificationBuilder image(String imageUrl) {
        this.imageUrl = imageUrl;
        return this;
    }

    // ===================== PRIORITY =====================

    /**
     * Set the notification priority.
     * {@link Priority#URGENT} notifications bypass rate limiting.
     */
    public BatchNotificationBuilder priority(Priority priority) {
        this.priority = priority != null ? priority : Priority.NORMAL;
        return this;
    }

    // ===================== SEND =====================

    /**
     * Send the notification to all recipients, collecting delivery receipts.
     * Each recipient gets an individual tracked send. Failures for one recipient
     * do not prevent delivery to the others.
     *
     * @return list of {@link DeliveryReceipt} — one per recipient
     */
    public List<DeliveryReceipt> send() {
        List<DeliveryReceipt> results = new ArrayList<>();

        if (recipients != null) {
            for (String recipient : recipients) {
                try {
                    NotificationBuilder b = hub.to(recipient);
                    applyCommon(b);
                    results.add(b.sendTracked());
                } catch (Exception e) {
                    results.add(DeliveryReceipt.builder()
                            .channelName(channels.isEmpty() ? "unknown" : channels.get(0))
                            .recipient(recipient)
                            .status(DeliveryStatus.FAILED)
                            .errorMessage(e.getMessage())
                            .build());
                }
            }
        } else if (notifiables != null) {
            for (Notifiable notifiable : notifiables) {
                try {
                    NotificationBuilder b = hub.to(notifiable);
                    applyCommon(b);
                    results.add(b.sendTracked());
                } catch (Exception e) {
                    results.add(DeliveryReceipt.builder()
                            .channelName(channels.isEmpty() ? "unknown" : channels.get(0))
                            .recipient(notifiable.getNotifyEmail())
                            .status(DeliveryStatus.FAILED)
                            .errorMessage(e.getMessage())
                            .build());
                }
            }
        }

        return results;
    }

    /**
     * Asynchronously send the notification to all recipients.
     *
     * @return CompletableFuture with a list of {@link DeliveryReceipt} — one per recipient
     */
    public CompletableFuture<List<DeliveryReceipt>> sendAsync() {
        return CompletableFuture.supplyAsync(this::send);
    }

    // ===================== INTERNAL =====================

    private void applyCommon(NotificationBuilder b) {
        for (String ch : channels) {
            b.via(Channel.custom(ch));
        }
        if (subject != null) {
            b.subject(subject);
        }
        if (templateName != null) {
            b.template(templateName);
        }
        if (rawContent != null) {
            b.content(rawContent);
        }
        b.params(params);
        b.priority(priority);
        if (imageUrl != null) {
            b.image(imageUrl);
        }
    }
}
