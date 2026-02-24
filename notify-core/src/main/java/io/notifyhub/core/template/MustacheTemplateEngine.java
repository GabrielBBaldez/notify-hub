package io.notifyhub.core.template;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.Map;

/**
 * Default template engine using Mustache.
 *
 * <p>Templates are loaded from the classpath at {@code templates/notify/}.</p>
 *
 * <p>File naming convention:</p>
 * <ul>
 *   <li>{@code templates/notify/order-confirmed.html} — email (HTML)</li>
 *   <li>{@code templates/notify/order-confirmed.txt} — SMS/WhatsApp (plain text)</li>
 * </ul>
 */
public class MustacheTemplateEngine implements TemplateEngine {

    private static final Logger log = LoggerFactory.getLogger(MustacheTemplateEngine.class);
    private static final String TEMPLATE_BASE_PATH = "templates/notify/";

    private final MustacheFactory mustacheFactory;

    public MustacheTemplateEngine() {
        this.mustacheFactory = new DefaultMustacheFactory(TEMPLATE_BASE_PATH);
    }

    @Override
    public String render(String templateName, String variant, Map<String, Object> params) {
        String fileName = templateName + "." + variant;

        // Try requested variant first, then fallback
        if (!exists(templateName, variant)) {
            String fallbackVariant = variant.equals("html") ? "txt" : "html";
            if (exists(templateName, fallbackVariant)) {
                log.debug("Template '{}' not found as .{}, using .{} fallback",
                        templateName, variant, fallbackVariant);
                fileName = templateName + "." + fallbackVariant;
            } else {
                throw new TemplateRenderException(templateName,
                        "Template not found: " + TEMPLATE_BASE_PATH + fileName);
            }
        }

        try {
            Mustache mustache = mustacheFactory.compile(fileName);
            StringWriter writer = new StringWriter();
            mustache.execute(writer, params).flush();
            return writer.toString();
        } catch (Exception e) {
            throw new TemplateRenderException(templateName,
                    "Failed to render template: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String templateName, String variant) {
        String resourcePath = TEMPLATE_BASE_PATH + templateName + "." + variant;
        InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception ignored) {
            }
            return true;
        }
        return false;
    }
}
