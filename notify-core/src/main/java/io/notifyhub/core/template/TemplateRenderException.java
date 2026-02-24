package io.notifyhub.core.template;

/**
 * Thrown when a template cannot be found or rendered.
 */
public class TemplateRenderException extends RuntimeException {

    private final String templateName;

    public TemplateRenderException(String templateName, String message) {
        super("Template '" + templateName + "': " + message);
        this.templateName = templateName;
    }

    public TemplateRenderException(String templateName, String message, Throwable cause) {
        super("Template '" + templateName + "': " + message, cause);
        this.templateName = templateName;
    }

    public String getTemplateName() {
        return templateName;
    }
}
