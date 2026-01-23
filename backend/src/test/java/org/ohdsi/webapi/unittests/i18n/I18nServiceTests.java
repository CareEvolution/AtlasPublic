package org.ohdsi.webapi.unittests.i18n;

import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.*;

import org.ohdsi.webapi.exceptions.FatalException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.ohdsi.webapi.i18n.I18nService;
import org.ohdsi.webapi.resourceutilities.StringResourceReader;

public class I18nServiceTests {
    @Test
    public void getAvailableLocales_InvalidLocaleFile_ReturnsLocaleList()
    {
        var stringResourceReader = Mockito.mock(StringResourceReader.class);
        when(stringResourceReader.getText("/i18n/locales.json"))
                .thenReturn("[{\"fred\": \"Fred Flintstone\", \"wilma\": \"Wilma Flintstone\"}]");

        var i18nService = new I18nService(stringResourceReader);
        assertThatExceptionOfType(FatalException.class)
                .isThrownBy(i18nService::getAvailableLocales);
    }

    @Test
    public void getAvailableLocales_ValidLocaleFile_ReturnsLocaleList()
    {
        var stringResourceReader = Mockito.mock(StringResourceReader.class);
        when(stringResourceReader.getText("/i18n/locales.json"))
                .thenReturn("[{\"code\": \"en\", \"name\": \"English\", \"default\": true }]");

        var i18nService = new I18nService(stringResourceReader);
        var locales = i18nService.getAvailableLocales();
        assertThat(locales).hasSize(1);
        assertThat(locales).extracting("code", "name", "default")
                .contains(tuple("en", "English", true));
    }

    @ParameterizedTest
    @ValueSource(strings={"XX", " ", ""})
    public void getLocalPhrases_GivenInvalidLocaleCode_Throws(String localeCode)
    {
        var stringReader = new StringResourceReader();
        var i18nService = new I18nService(stringReader);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> {i18nService.getLocalePhrases(localeCode);});
    }

    @Test
    public void getLocalPhrases_GivenNullLocaleCode_Throws()
    {
        var stringReader = new StringResourceReader();
        var i18nService = new I18nService(stringReader);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> {i18nService.getLocalePhrases(null);});
    }

    @ParameterizedTest
    @ValueSource(strings={"en", "EN", "ru", "RU"})
    public void getLocalPhrases_GivenValidLocaleCodeInAnyCase_ReturnsPhrases(String localeCode) {
        var stringResourceReader = Mockito.mock(StringResourceReader.class);
        var path = String.format("/i18n/messages_%s.json", localeCode.toLowerCase());
        when(stringResourceReader.getText(path))
                .thenReturn("phrases");

        var i18nService = new I18nService(stringResourceReader);
        var text = i18nService.getLocalePhrases(localeCode);
        assertThat(text).isEqualTo("phrases");
    }
}
