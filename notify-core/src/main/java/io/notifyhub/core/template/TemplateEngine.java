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
     * Check if a specific template variant exists.
     *
     * @param templateName template name
     * @param variant      "html" or "txt"
     * @return true if template file exists
     */
    boolean exists(String templateName, String variant);
}
