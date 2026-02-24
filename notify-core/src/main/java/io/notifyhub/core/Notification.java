package io.notifyhub.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable notification object containing all information needed to send.
 * Created internally by {@link NotificationBuilder}.
 */
public final class Notification {

    private final String recipient;
    private final String channelName;
    private final String subject;
    private final String templateName;
    private final String rawContent;
    private final Map<String, Object> params;
    private final List<Attachment> attachments;
    private final Priority priority;

    public Notification(String recipient, String channelName, String subject,
                 String templateName, String rawContent, Map<String, Object> params) {
        this(recipient, channelName, subject, templateName, rawContent, params,
                Collections.emptyList(), Priority.NORMAL);
    }

    public Notification(String recipient, String channelName, String subject,
                 String templateName, String rawContent, Map<String, Object> params,
                 List<Attachment> attachments, Priority priority) {
        this.recipient = recipient;
        this.channelName = channelName;
        this.subject = subject;
        this.templateName = templateName;
        this.rawContent = rawContent;
        this.params = params != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(params))
                : Collections.emptyMap();
        this.attachments = attachments != null
                ? Collections.unmodifiableList(attachments)
                : Collections.emptyList();
        this.priority = priority != null ? priority : Priority.NORMAL;
    }

    /** The recipient address (email, phone, push token, etc.) */
    public String getRecipient() {
        return recipient;
    }

    /** The channel name (e.g., "email", "sms", "slack") */
    public String getChannelName() {
        return channelName;
    }

    /** Email subject line. May be null for non-email channels. */
    public String getSubject() {
        return subject;
    }

    /** Template name to render. May be null if rawContent is used. */
    public String getTemplateName() {
        return templateName;
    }

    /** Raw content string. May be null if template is used. */
    public String getRawContent() {
        return rawContent;
    }

    /** Template parameters for variable substitution. */
    public Map<String, Object> getParams() {
        return params;
    }

    /** File attachments (primarily for email). Empty list if none. */
    public List<Attachment> getAttachments() {
        return attachments;
    }

    /** Notification priority level. Defaults to {@link Priority#NORMAL}. */
    public Priority getPriority() {
        return priority;
    }

    /**
     * Returns the rendered content — either from template or raw content.
     * The template is rendered by the NotifyHub before passing to the channel.
     */
    private String renderedContent;

    void setRenderedContent(String renderedContent) {
        this.renderedContent = renderedContent;
    }

    /** Returns the final rendered content ready to send. */
    public String getRenderedContent() {
        if (renderedContent != null) return renderedContent;
        if (rawContent != null) return rawContent;
        return "";
    }

    @Override
    public String toString() {
        return "Notification{" +
                "recipient='" + recipient + '\'' +
                ", channel='" + channelName + '\'' +
                ", template='" + templateName + '\'' +
                ", priority=" + priority +
                ", attachments=" + attachments.size() +
                '}';
    }
}
