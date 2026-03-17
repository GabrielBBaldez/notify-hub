package io.notifyhub.core.pipeline;

import io.notifyhub.core.Notification;
import io.notifyhub.core.NotificationSpec;
import io.notifyhub.core.channel.SendResult;
import io.notifyhub.core.template.TemplateEngine;
import io.notifyhub.core.template.VersionedTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pipeline handler that builds the {@link Notification} object and renders templates.
 *
 * <p>Constructs a {@link Notification} from the {@link NotificationSpec} snapshot,
 * renders the template if one is configured, and sets the result on the
 * {@link SendContext} via {@link SendContext#setNotification(Notification)}.</p>
 */
class TemplateHandler implements SendHandler {

    private static final Logger log = LoggerFactory.getLogger(TemplateHandler.class);

    private final TemplateEngine templateEngine;

    TemplateHandler(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public SendResult handle(SendContext context, SendHandler next) {
        NotificationSpec spec = context.getSpec();
        String channelName = context.getChannelName();

        Notification notification = spec.buildNotification(channelName);

        String templateName = spec.getTemplateName();
        if (templateName != null && templateEngine != null) {
            String variant = resolveVariant(channelName);
            String rendered;

            String templateVersion = spec.getTemplateVersion();
            if (templateVersion != null && templateEngine instanceof VersionedTemplateEngine vte) {
                rendered = vte.render(templateName, templateVersion, variant,
                        spec.getParams(), spec.getLocale());
            } else {
                rendered = templateEngine.render(templateName, variant,
                        spec.getParams(), spec.getLocale());
            }

            notification = spec.applyRenderedContent(notification, rendered);
            log.debug("Rendered template '{}' for channel '{}'", templateName, channelName);
        }

        context.setNotification(notification);
        return next.handle(context, null);
    }

    private String resolveVariant(String channelName) {
        return switch (channelName) {
            case "email" -> "html";
            default -> "txt";
        };
    }
}
