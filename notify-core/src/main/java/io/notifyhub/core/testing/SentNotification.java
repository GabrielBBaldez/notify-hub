package io.notifyhub.core.testing;

/**
 * Captures a single notification that was sent through a {@link TestNotifyHub}.
 * Used in test assertions to verify notification content and routing.
 */
public record SentNotification(
    String channel,
    String recipient,
    String subject,
    String content,
    String templateName
) {}
