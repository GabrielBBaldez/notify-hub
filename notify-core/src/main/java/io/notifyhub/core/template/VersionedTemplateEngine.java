package io.notifyhub.core.template;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Extended template engine with version support.
 *
 * <p>Supports loading specific versions of templates following the naming convention:</p>
 * <ul>
 *   <li>{@code order-confirmed.html} — default version</li>
 *   <li>{@code order-confirmed@v1.html} — version v1</li>
 *   <li>{@code order-confirmed@v2.html} — version v2</li>
 *   <li>{@code order-confirmed_pt_BR@v2.html} — version v2 with i18n</li>
 * </ul>
 *
 * <p>Resolution order:</p>
 * <ol>
 *   <li>{@code name@version_locale.variant}</li>
 *   <li>{@code name@version.variant}</li>
 *   <li>{@code name_locale.variant} (fallback to default version)</li>
 *   <li>{@code name.variant} (fallback)</li>
 * </ol>
 *
 * <pre>{@code
 * VersionedTemplateEngine engine = ...;
 * String content = engine.render("welcome", "v2", "html", params, locale);
 * List<String> versions = engine.getAvailableVersions("welcome", "html");
 * }</pre>
 */
public interface VersionedTemplateEngine extends TemplateEngine {

    /**
     * Render a specific version of a template.
     *
     * @param templateName template name (e.g., "order-confirmed")
     * @param version      template version (e.g., "v2"), or null for default
     * @param variant      "html" or "txt"
     * @param params       template parameters
     * @param locale       locale for i18n (may be null)
     * @return rendered content
     */
    String render(String templateName, String version, String variant,
                  Map<String, Object> params, Locale locale);

    /**
     * Get all available versions for a template.
     *
     * @param templateName template name
     * @param variant      "html" or "txt"
     * @return list of version strings (e.g., ["v1", "v2"])
     */
    List<String> getAvailableVersions(String templateName, String variant);

    /**
     * Check if a specific version of a template exists.
     *
     * @param templateName template name
     * @param version      version string (e.g., "v2")
     * @param variant      "html" or "txt"
     * @return true if the versioned template exists
     */
    boolean existsVersion(String templateName, String version, String variant);
}
