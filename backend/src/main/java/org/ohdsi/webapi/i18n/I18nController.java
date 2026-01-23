package org.ohdsi.webapi.i18n;

import org.ohdsi.webapi.webapiproperties.WebApiProperties;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;

import java.util.List;

/**
 * REST API for working with internationalization (i18n) resources. This controller provides endpoints for retrieving
 * locale-specific phrases and the list of available locales. The phrases are used by the client to display localized content.
 */
@RestController
@RequestMapping("i18n")
@RequestScope
public class I18nController {
    private final WebApiProperties webApiProperties;
    private final I18nService i18nService;

    public I18nController(WebApiProperties webApiProperties, I18nService i18nService) {
        this.webApiProperties = webApiProperties;
        this.i18nService = i18nService;
    }

    /**
     * Get the locale-specific phrases for a given locale code. If the locale code is not provided or is empty, 
     * the default locale phrases will be returned.
     * @param lang
     *   The two-character locale code (e.g., "en", "fr"). If not provided, the default locale will be used.
     * @return
     *  A JSON string containing the locale-specific phrases.
     */
    @GetMapping()
    public String getLocalePhrases(@RequestParam String lang) {
        var defaultLocale = webApiProperties.i18n().defaultLocale();

        // Return default locale phrases if empty or all blank query parameter
        if (!StringUtils.hasText(lang)) {
            return i18nService.getLocalePhrases(defaultLocale);
        }

        if (!webApiProperties.i18n().enabled()) {
            return i18nService.getLocalePhrases(defaultLocale);
        }

        if (isLocaleSupported(lang)) {
            return i18nService.getLocalePhrases(lang);
        }
        return i18nService.getLocalePhrases(defaultLocale);
    }

    /**
     * Get the list of available locales supported by the application.
     * @return
     * A list of AtlasLocale objects representing the available locales.
     */
    @GetMapping("/locales")
    public List<AtlasLocale> getLocales() {
        return i18nService.getAvailableLocales();
    }

    private boolean isLocaleSupported(String twoCharLocaleCode) {
        var atlasLocales = i18nService.getAvailableLocales();
        return atlasLocales.stream().anyMatch(l -> twoCharLocaleCode.equalsIgnoreCase(l.code()));
    }
}
