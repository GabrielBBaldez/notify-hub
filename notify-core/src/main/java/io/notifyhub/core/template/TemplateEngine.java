package io.notifyhub.core.template;

import java.util.Map;

/**
 * Interface for rendering notification templates.
 * Default implementation uses Mustache. Can be replaced with
 * Thymeleaf, FreeMarker, or any custom engine.
 */
public interface TemplateEngine {

    /**
     * Render a template with the given parameters.
     *
     * @param templateName template name (without extension, e.g., "order-confirmed")
     * @param variant      template variant — "html" for email, "txt" for SMS/WhatsApp
     * @param params       key-value parameters for substitution
     * @return rendered content string
     * @throws TemplateRenderException if template not found or rendering fails
     */
    String render(String templateName, String variant, Map<String, Object> params);

    /**
     * Render a template with locale support for i18n.
     * Default implementation ignores the locale and delegates to the 3-arg render.
     *
     * @param templateName template name (without extension)
     * @param variant      template variant — "html" for email, "txt" for SMS/WhatsApp
     * @param params       key-value parameters for substitution
     * @param locale       the locale for template resolution (may be null)
     * @return rendered content string
     */
    default String render(String templateName, String variant, Map<String, Object> params, java.util.Locale locale) {
        return render(templateName, variant, params);
    }

    /**
     * Check if a specific template variant exists.
     *
     * @param templateName template name
     * @param variant      "html" or "txt"
     * @return true if template file exists
     */
    boolean exists(String templateName, String variant);
}
