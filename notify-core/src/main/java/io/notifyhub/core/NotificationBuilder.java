package io.notifyhub.core;

import io.notifyhub.core.retry.RetryPolicy;

import java.util.*;

/**
 * Fluent builder for constructing and sending notifications.
 *
 * <pre>{@code
 * notify.to(user)
 *     .via(Channel.EMAIL)
 *     .fallback(Channel.SMS)
 *     .subject("Order confirmed")
 *     .template("order-confirmed")
 *     .param("orderId", order.getId())
 *     .param("total", order.getTotal())
 *     .send();
 * }</pre>
 */
public class NotificationBuilder {

    private final NotifyHub hub;

    // Recipient
    private String recipientEmail;
    private String recipientPhone;
    private String recipientPushToken;
    private String recipientName;

    // Channels
    private final List<String> channels = new ArrayList<>();
    private final List<String> fallbackChannels = new ArrayList<>();

    // Content
    private String subject;
    private String templateName;
    private String rawContent;
    private final Map<String, Object> params = new LinkedHashMap<>();

    // Retry
    private RetryPolicy retryPolicy;

    NotificationBuilder(NotifyHub hub) {
        this.hub = hub;
    }

    // ===================== RECIPIENT =====================

    /** Set recipient from a Notifiable entity (User, Customer, etc.) */
    public NotificationBuilder to(Notifiable notifiable) {
        this.recipientEmail = notifiable.getNotifyEmail();
        this.recipientPhone = notifiable.getNotifyPhone();
        this.recipientPushToken = notifiable.getNotifyPushToken();
        this.recipientName = notifiable.getNotifyName();
        if (recipientName != null) {
            this.params.put("recipientName", recipientName);
        }
        return this;
    }

    /** Set recipient by email address directly. */
    public NotificationBuilder to(String email) {
        this.recipientEmail = email;
        return this;
    }

    /** Set recipient by phone number directly. */
    public NotificationBuilder toPhone(String phone) {
        this.recipientPhone = phone;
        return this;
    }

    // ===================== CHANNELS =====================

    /** Set the primary channel(s) to send through. */
    public NotificationBuilder via(Channel... channels) {
        for (Channel ch : channels) {
            this.channels.add(ch.name().toLowerCase());
        }
        return this;
    }

    /** Set a custom channel by reference. */
    public NotificationBuilder via(ChannelRef ref) {
        this.channels.add(ref.getName());
        return this;
    }

    /** Add a fallback channel if the primary fails. Multiple fallbacks are tried in order. */
    public NotificationBuilder fallback(Channel channel) {
        this.fallbackChannels.add(channel.name().toLowerCase());
        return this;
    }

    /** Add a custom fallback channel. */
    public NotificationBuilder fallback(ChannelRef ref) {
        this.fallbackChannels.add(ref.getName());
        return this;
    }

    // ===================== CONTENT =====================

    /** Set the email subject line. */
    public NotificationBuilder subject(String subject) {
        this.subject = subject;
        return this;
    }

    /** Set the template name (loaded from resources/templates/notify/). */
    public NotificationBuilder template(String templateName) {
        this.templateName = templateName;
        return this;
    }

    /** Set raw content directly (no template). */
    public NotificationBuilder content(String content) {
        this.rawContent = content;
        return this;
    }

    /** Add a template parameter. */
    public NotificationBuilder param(String key, Object value) {
        this.params.put(key, value);
        return this;
    }

    /** Add multiple template parameters. */
    public NotificationBuilder params(Map<String, Object> params) {
        this.params.putAll(params);
        return this;
    }

    // ===================== RETRY =====================

    /** Set custom retry policy for this notification. */
    public NotificationBuilder retry(RetryPolicy policy) {
        this.retryPolicy = policy;
        return this;
    }

    /** Set retry with exponential backoff. */
    public NotificationBuilder retry(int maxAttempts) {
        this.retryPolicy = RetryPolicy.exponential(maxAttempts);
        return this;
    }

    // ===================== SEND =====================

    /**
     * Send the notification through the first specified channel.
     * If it fails and fallbacks are configured, tries each fallback in order.
     */
    public void send() {
        validate();
        hub.execute(this);
    }

    /**
     * Send through ALL specified channels simultaneously (not just the first).
     * Failures on individual channels don't block others.
     */
    public void sendAll() {
        validate();
        hub.executeAll(this);
    }

    // ===================== INTERNAL =====================

    private void validate() {
        if (channels.isEmpty()) {
            throw new IllegalStateException("No channel specified. Use .via(Channel.EMAIL) to set a channel.");
        }
        if (templateName == null && rawContent == null) {
            throw new IllegalStateException("No content specified. Use .template(\"name\") or .content(\"text\").");
        }
    }

    /** Resolve the recipient address for a given channel name. */
    String resolveRecipient(String channelName) {
        return switch (channelName) {
            case "email" -> recipientEmail;
            case "sms", "whatsapp" -> recipientPhone;
            case "push" -> recipientPushToken;
            default -> recipientEmail != null ? recipientEmail : recipientPhone;
        };
    }

    List<String> getChannels() {
        return Collections.unmodifiableList(channels);
    }

    List<String> getFallbackChannels() {
        return Collections.unmodifiableList(fallbackChannels);
    }

    String getSubject() {
        return subject;
    }

    String getTemplateName() {
        return templateName;
    }

    String getRawContent() {
        return rawContent;
    }

    Map<String, Object> getParams() {
        return Collections.unmodifiableMap(params);
    }

    RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }
}
