package io.notifyhub.core.template;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Default template engine using Mustache, with version support.
 *
 * <p>Templates are loaded from the classpath at {@code templates/notify/}.</p>
 *
 * <p>File naming convention:</p>
 * <ul>
 *   <li>{@code templates/notify/order-confirmed.html} — default version, email (HTML)</li>
 *   <li>{@code templates/notify/order-confirmed.txt} — default version, SMS (plain text)</li>
 *   <li>{@code templates/notify/order-confirmed@v2.html} — version v2, email</li>
 *   <li>{@code templates/notify/order-confirmed_pt_BR@v2.html} — version v2, i18n</li>
 * </ul>
 */
public class MustacheTemplateEngine implements VersionedTemplateEngine {

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
    public String render(String templateName, String variant, Map<String, Object> params, Locale locale) {
        if (locale != null) {
            // Try locale-specific templates: e.g., order-confirmed_pt_BR, then order-confirmed_pt
            String lang = locale.getLanguage();
            String country = locale.getCountry();

            if (country != null && !country.isEmpty()) {
                String localized = templateName + "_" + lang + "_" + country;
                if (exists(localized, variant)) {
                    return render(localized, variant, params);
                }
            }

            if (lang != null && !lang.isEmpty()) {
                String localized = templateName + "_" + lang;
                if (exists(localized, variant)) {
                    return render(localized, variant, params);
                }
            }
        }

        // Fallback to default (no locale suffix)
        return render(templateName, variant, params);
    }

    // ===================== VERSIONED TEMPLATE ENGINE =====================

    @Override
    public String render(String templateName, String version, String variant,
                         Map<String, Object> params, Locale locale) {
        if (version == null || version.isBlank()) {
            // No version specified — use normal locale-aware resolution
            return render(templateName, variant, params, locale);
        }

        // Try versioned + localized: name_locale@version.variant
        if (locale != null) {
            String lang = locale.getLanguage();
            String country = locale.getCountry();

            if (country != null && !country.isEmpty()) {
                String versionedLocalized = templateName + "_" + lang + "_" + country + "@" + version;
                if (exists(versionedLocalized, variant)) {
                    return render(versionedLocalized, variant, params);
                }
            }

            if (lang != null && !lang.isEmpty()) {
                String versionedLocalized = templateName + "_" + lang + "@" + version;
                if (exists(versionedLocalized, variant)) {
                    return render(versionedLocalized, variant, params);
                }
            }
        }

        // Try versioned without locale: name@version.variant
        String versioned = templateName + "@" + version;
        if (exists(versioned, variant)) {
            return render(versioned, variant, params);
        }

        // Fallback: normal locale-aware resolution (without version)
        log.debug("Versioned template '{}@{}' not found, falling back to default", templateName, version);
        return render(templateName, variant, params, locale);
    }

    @Override
    public List<String> getAvailableVersions(String templateName, String variant) {
        // Scan common versions: v1..v10
        List<String> versions = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            String version = "v" + i;
            if (existsVersion(templateName, version, variant)) {
                versions.add(version);
            }
        }
        return versions;
    }

    @Override
    public boolean existsVersion(String templateName, String version, String variant) {
        String versioned = templateName + "@" + version;
        return exists(versioned, variant);
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
