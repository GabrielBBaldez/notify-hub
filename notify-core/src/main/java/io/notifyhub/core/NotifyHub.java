package io.notifyhub.core;

import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
import io.notifyhub.core.retry.RetryPolicy;
import io.notifyhub.core.template.TemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main entry point for sending notifications.
 *
 * <pre>{@code
 * NotifyHub notify = NotifyHub.builder()
 *     .templateEngine(new MustacheTemplateEngine())
 *     .channel(new SmtpEmailChannel(config))
 *     .build();
 *
 * notify.to(user)
 *     .via(Channel.EMAIL)
 *     .subject("Welcome!")
 *     .template("welcome")
 *     .param("name", user.getName())
 *     .send();
 * }</pre>
 */
public class NotifyHub {

    private static final Logger log = LoggerFactory.getLogger(NotifyHub.class);

    private final Map<String, NotificationChannel> channels;
    private final TemplateEngine templateEngine;
    private final RetryPolicy defaultRetryPolicy;
    private final List<NotificationListener> listeners;

    private NotifyHub(Builder builder) {
        this.channels = new ConcurrentHashMap<>(builder.channels);
        this.templateEngine = builder.templateEngine;
        this.defaultRetryPolicy = builder.defaultRetryPolicy != null
                ? builder.defaultRetryPolicy
                : RetryPolicy.none();
        this.listeners = new ArrayList<>(builder.listeners);
    }

    // ===================== FLUENT API ENTRY POINTS =====================

    /**
     * Start building a notification to a Notifiable recipient.
     *
     * <pre>{@code
     * notify.to(user).via(EMAIL).template("welcome").send();
     * }</pre>
     */
    public NotificationBuilder to(Notifiable notifiable) {
        return new NotificationBuilder(this).to(notifiable);
    }

    /**
     * Start building a notification to an email address.
     *
     * <pre>{@code
     * notify.to("user@example.com").via(EMAIL).template("welcome").send();
     * }</pre>
     */
    public NotificationBuilder to(String email) {
        return new NotificationBuilder(this).to(email);
    }

    /**
     * Start building a notification to a phone number.
     *
     * <pre>{@code
     * notify.toPhone("+5548999999999").via(SMS).content("Your code: 1234").send();
     * }</pre>
     */
    public NotificationBuilder toPhone(String phone) {
        return new NotificationBuilder(this).toPhone(phone);
    }

    // ===================== CHANNEL MANAGEMENT =====================

    /** Register a new channel at runtime. */
    public void registerChannel(NotificationChannel channel) {
        channels.put(channel.getName().toLowerCase(), channel);
        log.info("Registered notification channel: {}", channel.getName());
    }

    /** Get a registered channel by name. */
    public Optional<NotificationChannel> getChannel(String name) {
        return Optional.ofNullable(channels.get(name.toLowerCase()));
    }

    /** Get all registered channel names. */
    public Set<String> getRegisteredChannels() {
        return Collections.unmodifiableSet(channels.keySet());
    }

    // ===================== EXECUTION =====================

    /**
     * Execute notification: send through first channel, try fallbacks on failure.
     * Called internally by NotificationBuilder.send().
     */
    void execute(NotificationBuilder builder) {
        List<String> allChannels = new ArrayList<>(builder.getChannels());
        allChannels.addAll(builder.getFallbackChannels());

        NotificationSendException lastException = null;

        for (String channelName : allChannels) {
            try {
                sendToChannel(channelName, builder);
                return; // success — stop trying
            } catch (NotificationSendException e) {
                lastException = e;
                log.warn("Channel '{}' failed: {}. Trying next fallback...",
                        channelName, e.getMessage());
                notifyListeners(channelName, builder, e);
            }
        }

        // All channels failed
        if (lastException != null) {
            log.error("All channels failed for notification. Template: '{}', Recipient channels tried: {}",
                    builder.getTemplateName(), allChannels);
            throw lastException;
        }
    }

    /**
     * Execute notification on ALL channels simultaneously.
     * Called internally by NotificationBuilder.sendAll().
     */
    void executeAll(NotificationBuilder builder) {
        List<String> channelNames = builder.getChannels();
        List<NotificationSendException> failures = new ArrayList<>();

        for (String channelName : channelNames) {
            try {
                sendToChannel(channelName, builder);
            } catch (NotificationSendException e) {
                failures.add(e);
                log.warn("Channel '{}' failed during sendAll: {}", channelName, e.getMessage());
                notifyListeners(channelName, builder, e);
            }
        }

        if (failures.size() == channelNames.size()) {
            throw new NotificationSendException("all",
                    "All " + failures.size() + " channels failed");
        }
    }

    private void sendToChannel(String channelName, NotificationBuilder builder) {
        NotificationChannel channel = channels.get(channelName);
        if (channel == null) {
            throw new NotificationSendException(channelName,
                    "Channel '" + channelName + "' not registered. Available: " + channels.keySet());
        }

        String recipient = builder.resolveRecipient(channelName);
        if (recipient == null || recipient.isBlank()) {
            throw new NotificationSendException(channelName,
                    "No recipient address available for channel '" + channelName + "'");
        }

        // Render template
        String renderedContent = null;
        if (builder.getTemplateName() != null && templateEngine != null) {
            String variant = channelName.equals("email") ? "html" : "txt";
            renderedContent = templateEngine.render(
                    builder.getTemplateName(), variant, builder.getParams());
        }

        // Build notification object
        Notification notification = new Notification(
                recipient, channelName, builder.getSubject(),
                builder.getTemplateName(), builder.getRawContent(),
                builder.getParams()
        );
        if (renderedContent != null) {
            notification.setRenderedContent(renderedContent);
        }

        // Send with retry
        RetryPolicy policy = builder.getRetryPolicy() != null
                ? builder.getRetryPolicy()
                : defaultRetryPolicy;

        sendWithRetry(channel, notification, policy);

        log.info("Notification sent via '{}' to '{}'", channelName, recipient);
        notifyListenersSuccess(channelName, builder);
    }

    private void sendWithRetry(NotificationChannel channel, Notification notification,
                                RetryPolicy policy) {
        int attempts = policy.getMaxAttempts();
        NotificationSendException lastException = null;

        for (int i = 0; i < attempts; i++) {
            try {
                if (i > 0) {
                    long delayMs = policy.getDelayForAttempt(i - 1).toMillis();
                    log.debug("Retry attempt {}/{} for channel '{}' after {}ms",
                            i + 1, attempts, channel.getName(), delayMs);
                    Thread.sleep(delayMs);
                }
                channel.send(notification);
                return; // success
            } catch (NotificationSendException e) {
                lastException = e;
                log.warn("Attempt {}/{} failed for channel '{}': {}",
                        i + 1, attempts, channel.getName(), e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new NotificationSendException(channel.getName(),
                        "Retry interrupted", e);
            } catch (Exception e) {
                lastException = new NotificationSendException(channel.getName(),
                        "Unexpected error: " + e.getMessage(), e);
            }
        }

        throw lastException;
    }

    // ===================== LISTENERS =====================

    private void notifyListeners(String channelName, NotificationBuilder builder, Exception error) {
        for (NotificationListener listener : listeners) {
            try {
                listener.onFailure(channelName, builder.getTemplateName(), error);
            } catch (Exception e) {
                log.warn("Listener error: {}", e.getMessage());
            }
        }
    }

    private void notifyListenersSuccess(String channelName, NotificationBuilder builder) {
        for (NotificationListener listener : listeners) {
            try {
                listener.onSuccess(channelName, builder.getTemplateName());
            } catch (Exception e) {
                log.warn("Listener error: {}", e.getMessage());
            }
        }
    }

    // ===================== BUILDER =====================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<String, NotificationChannel> channels = new LinkedHashMap<>();
        private TemplateEngine templateEngine;
        private RetryPolicy defaultRetryPolicy;
        private final List<NotificationListener> listeners = new ArrayList<>();

        public Builder channel(NotificationChannel channel) {
            this.channels.put(channel.getName().toLowerCase(), channel);
            return this;
        }

        public Builder channels(Collection<? extends NotificationChannel> channels) {
            for (NotificationChannel ch : channels) {
                this.channels.put(ch.getName().toLowerCase(), ch);
            }
            return this;
        }

        public Builder templateEngine(TemplateEngine templateEngine) {
            this.templateEngine = templateEngine;
            return this;
        }

        public Builder defaultRetryPolicy(RetryPolicy retryPolicy) {
            this.defaultRetryPolicy = retryPolicy;
            return this;
        }

        public Builder listener(NotificationListener listener) {
            this.listeners.add(listener);
            return this;
        }

        public NotifyHub build() {
            return new NotifyHub(this);
        }
    }
}
