package org.ohdsi.webapi.i18n;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ohdsi.webapi.exceptions.FatalException;
import org.ohdsi.webapi.resourceutilities.StringResourceReader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class I18nService {
    private final StringResourceReader stringReader;

    public I18nService(StringResourceReader stringReader) {
        this.stringReader = stringReader;
    }

    public List<AtlasLocale> getAvailableLocales() {
        var jsonText = this.stringReader.getText("/i18n/locales.json");
        try {
            var objectMapper = new ObjectMapper();
            return objectMapper.readValue(jsonText, new TypeReference<List<AtlasLocale>>() {});
        } catch (JsonProcessingException e) {
            throw new FatalException("Cannot deserialize locale list", e);
        }
    }

    public String getLocalePhrases(String twoCharLocaleCode) {
        if (!StringUtils.hasText(twoCharLocaleCode)) {
            throw new IllegalArgumentException("Locale code is null or empty");
        }

        // Our locale codes are in lower case.  We're going to read a resource with a
        // case-sensitive name so make sure the code is in the proper case.
        var resourcePath = String.format("/i18n/messages_%s.json", twoCharLocaleCode.toLowerCase());
        return this.stringReader.getText(resourcePath);
    }
}
