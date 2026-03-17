package io.notifyhub.core;

import io.notifyhub.core.attachment.Attachment;

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
    private final String imageUrl;

    public Notification(String recipient, String channelName, String subject,
                 String templateName, String rawContent, Map<String, Object> params) {
        this(recipient, channelName, subject, templateName, rawContent, params,
                Collections.emptyList(), Priority.NORMAL, null);
    }

    public Notification(String recipient, String channelName, String subject,
                 String templateName, String rawContent, Map<String, Object> params,
                 List<Attachment> attachments, Priority priority) {
        this(recipient, channelName, subject, templateName, rawContent, params,
                attachments, priority, null);
    }

    public Notification(String recipient, String channelName, String subject,
                 String templateName, String rawContent, Map<String, Object> params,
                 List<Attachment> attachments, Priority priority, String imageUrl) {
        this(recipient, channelName, subject, templateName, rawContent, params,
                attachments, priority, imageUrl, null);
    }

    Notification(String recipient, String channelName, String subject,
                 String templateName, String rawContent, Map<String, Object> params,
                 List<Attachment> attachments, Priority priority, String imageUrl,
                 String renderedContent) {
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
        this.imageUrl = imageUrl;
        this.renderedContent = renderedContent;
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

    /** Image URL to embed in the notification. May be null if no image. */
    public String getImageUrl() {
        return imageUrl;
    }

    private final String renderedContent;

    /**
     * Returns a copy of this notification with the rendered content set.
     * Used by NotifyHub after template rendering.
     */
    Notification withRenderedContent(String renderedContent) {
        return new Notification(this.recipient, this.channelName, this.subject,
                this.templateName, this.rawContent, this.params,
                this.attachments, this.priority, this.imageUrl, renderedContent);
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
                (imageUrl != null ? ", imageUrl='" + imageUrl + '\'' : "") +
                '}';
    }
}
