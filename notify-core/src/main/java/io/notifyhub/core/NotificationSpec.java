package io.notifyhub.core;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A snapshot of the data inside a {@link NotificationBuilder}, exposing the
 * package-private builder fields to classes in other packages (e.g., the pipeline).
 *
 * <p>This class lives in {@code io.notifyhub.core} so it can read the package-private
 * getters of {@link NotificationBuilder} and {@link Notification}. It then exposes
 * all values through public methods, bridging the gap for pipeline handlers.</p>
 */
public final class NotificationSpec {

    private final String recipient;
    private final String subject;
    private final String templateName;
    private final String rawContent;
    private final Map<String, Object> params;
    private final List<Attachment> attachments;
    private final Priority priority;
    private final String imageUrl;
    private final Locale locale;
    private final String templateVersion;
    private final String deduplicationKey;
    private final String deduplicationHash;

    /**
     * Create a snapshot from the given builder, resolved for the given channel name.
     *
     * @param builder     the notification builder
     * @param channelName the target channel name (used to resolve the recipient)
     */
    public NotificationSpec(NotificationBuilder builder, String channelName) {
        this.recipient = builder.resolveRecipient(channelName);
        this.subject = builder.getSubject();
        this.templateName = builder.getTemplateName();
        this.rawContent = builder.getRawContent();
        this.params = builder.getParams();
        this.attachments = builder.getAttachments();
        this.priority = builder.getPriority();
        this.imageUrl = builder.getImageUrl();
        this.locale = builder.getLocale();
        this.templateVersion = builder.getTemplateVersion();
        this.deduplicationKey = builder.getDeduplicationKey();
        this.deduplicationHash = builder.computeDeduplicationHash();
    }

    public String getRecipient() { return recipient; }
    public String getSubject() { return subject; }
    public String getTemplateName() { return templateName; }
    public String getRawContent() { return rawContent; }
    public Map<String, Object> getParams() { return params; }
    public List<Attachment> getAttachments() { return attachments; }
    public Priority getPriority() { return priority; }
    public String getImageUrl() { return imageUrl; }
    public Locale getLocale() { return locale; }
    public String getTemplateVersion() { return templateVersion; }
    public String getDeduplicationKey() { return deduplicationKey; }
    public String getDeduplicationHash() { return deduplicationHash; }

    /**
     * Build a {@link Notification} from this spec for the given channel name,
     * and apply rendered content. Package-private — used by the pipeline.
     */
    public Notification buildNotification(String channelName) {
        return new Notification(
                recipient, channelName, subject, templateName, rawContent,
                params, attachments, priority, imageUrl);
    }

    /**
     * Apply rendered content to a notification. Package-private access to
     * {@link Notification#withRenderedContent(String)}.
     */
    public Notification applyRenderedContent(Notification notification, String rendered) {
        return notification.withRenderedContent(rendered);
    }
}
