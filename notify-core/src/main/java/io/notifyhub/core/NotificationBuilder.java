package io.notifyhub.core;

import io.notifyhub.core.retry.RetryPolicy;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Fluent builder for constructing and sending notifications.
 *
 * <pre>{@code
 * // Simple send
 * notify.to(user)
 *     .via(Channel.EMAIL)
 *     .fallback(Channel.SMS)
 *     .subject("Order confirmed")
 *     .template("order-confirmed")
 *     .param("orderId", order.getId())
 *     .send();
 *
 * // Tracked send
 * DeliveryReceipt receipt = notify.to(user)
 *     .via(Channel.EMAIL)
 *     .content("Hello!")
 *     .sendTracked();
 *
 * // Scheduled send (fires in 30 minutes)
 * ScheduledNotification scheduled = notify.to(user)
 *     .via(Channel.EMAIL)
 *     .content("Reminder!")
 *     .schedule(Duration.ofMinutes(30));
 *
 * // Scheduled for specific date/time
 * ScheduledNotification scheduled = notify.to(user)
 *     .via(Channel.EMAIL)
 *     .content("Happy Birthday!")
 *     .scheduleAt(LocalDateTime.of(2024, 12, 25, 9, 0));
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

    /**
     * Asynchronously send the notification through the first specified channel.
     * If it fails and fallbacks are configured, tries each fallback in order.
     *
     * @return CompletableFuture that completes when the notification is sent (or all fallbacks fail)
     */
    public CompletableFuture<Void> sendAsync() {
        validate();
        return hub.executeAsync(this);
    }

    /**
     * Asynchronously send through ALL specified channels.
     * Failures on individual channels don't block others.
     *
     * @return CompletableFuture that completes when all channels have been attempted
     */
    public CompletableFuture<Void> sendAllAsync() {
        validate();
        return hub.executeAllAsync(this);
    }

    // ===================== TRACKED SEND =====================

    /**
     * Send the notification and return a delivery receipt with tracking information.
     * The receipt contains the delivery status, channel, recipient, and timestamp.
     *
     * <p>If a {@link NotificationTracker} is configured on the hub, the receipt
     * is automatically recorded for later querying.</p>
     *
     * @return a {@link DeliveryReceipt} with the delivery outcome
     * @throws io.notifyhub.core.channel.NotificationSendException if all channels fail
     */
    public DeliveryReceipt sendTracked() {
        validate();
        return hub.executeTracked(this);
    }

    /**
     * Send through ALL specified channels and return delivery receipts for each.
     * Each channel gets its own receipt (SENT or FAILED).
     *
     * @return list of {@link DeliveryReceipt} — one per channel
     * @throws io.notifyhub.core.channel.NotificationSendException if ALL channels fail
     */
    public List<DeliveryReceipt> sendAllTracked() {
        validate();
        return hub.executeAllTracked(this);
    }

    // ===================== SCHEDULED SEND =====================

    /**
     * Schedule the notification to be sent after a delay.
     *
     * <pre>{@code
     * ScheduledNotification scheduled = notify.to("user@test.com")
     *     .via(Channel.EMAIL)
     *     .content("Reminder: meeting in 30 minutes!")
     *     .schedule(Duration.ofMinutes(30));
     *
     * // Cancel if needed
     * scheduled.cancel();
     * }</pre>
     *
     * @param delay how long to wait before sending
     * @return a {@link ScheduledNotification} handle to inspect or cancel
     */
    public ScheduledNotification schedule(Duration delay) {
        validate();
        if (delay == null || delay.isNegative()) {
            throw new IllegalArgumentException("Delay must be a positive duration");
        }
        return hub.executeScheduled(this, delay);
    }

    /**
     * Schedule the notification to be sent at a specific date and time.
     * Uses the system default timezone.
     *
     * <pre>{@code
     * ScheduledNotification scheduled = notify.to("user@test.com")
     *     .via(Channel.EMAIL)
     *     .content("Happy Birthday!")
     *     .scheduleAt(LocalDateTime.of(2024, 12, 25, 9, 0));
     * }</pre>
     *
     * @param dateTime when to send the notification
     * @return a {@link ScheduledNotification} handle to inspect or cancel
     * @throws IllegalArgumentException if dateTime is in the past
     */
    public ScheduledNotification scheduleAt(LocalDateTime dateTime) {
        return scheduleAt(dateTime, ZoneId.systemDefault());
    }

    /**
     * Schedule the notification to be sent at a specific date and time in a given timezone.
     *
     * @param dateTime when to send the notification
     * @param zoneId   the timezone for the dateTime
     * @return a {@link ScheduledNotification} handle to inspect or cancel
     * @throws IllegalArgumentException if dateTime is in the past
     */
    public ScheduledNotification scheduleAt(LocalDateTime dateTime, ZoneId zoneId) {
        validate();
        if (dateTime == null) {
            throw new IllegalArgumentException("DateTime must not be null");
        }

        java.time.Instant target = dateTime.atZone(zoneId).toInstant();
        Duration delay = Duration.between(java.time.Instant.now(), target);

        if (delay.isNegative()) {
            throw new IllegalArgumentException(
                    "Cannot schedule in the past. Provided: " + dateTime + " (zone: " + zoneId + ")");
        }

        return hub.executeScheduled(this, delay);
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
            case "slack", "telegram", "discord" -> recipientEmail;
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
