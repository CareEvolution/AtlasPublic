package org.ohdsi.webapi.smoketests.i18n;

import static io.restassured.RestAssured.*;

import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.ohdsi.webapi.WebapiApplication;
import org.ohdsi.webapi.i18n.AtlasLocale;

import static org.assertj.core.api.Assertions.*;

import org.ohdsi.webapi.resourceutilities.StringResourceReader;
import org.ohdsi.webapi.smoketests.AbstractIntegrationTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = WebapiApplication.class)
@TestPropertySource(locations = "/application-i18n-disabled.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Testcontainers
public class I18nDisabledFeatureIT extends AbstractIntegrationTest {
    @Value("${webapi.i18n.defaultLocale}")
    private String i18nDefaultLocaleCode;

    @ParameterizedTest
    @ValueSource(strings = { "en", "EN", "ru", "RU", "kO", "zh" })
    public void getLocalePhrases_GivenI18nDisabledAndAnyCaseLocaleCode_ReturnsDefaultLocalePhrases(
            String twoCharLocaleCode) {
        var url = "/WebAPI/i18n";
        var phraseText =
                given()
                        .queryParam("lang", twoCharLocaleCode)
                        .when()
                        .get(url)
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .contentType(ContentType.TEXT)
                        .extract()
                        .asString();

        var resultMd5 = generateMd5(phraseText);
        var stringReader = new StringResourceReader();
        var path = String.format("/i18n/messages_%s.json", this.i18nDefaultLocaleCode.toLowerCase());
        var expectedPhraseText = stringReader.getText(path);
        var expectedMd5 = generateMd5(expectedPhraseText);
        assertThat(resultMd5).isEqualTo(expectedMd5);
    }

    @Test
    public void getLocalePhrases_GivenI18nDisabledAndBlankLocaleCode_ReturnsDefaultLocalePhrases() {
        var url = "/WebAPI/i18n";
        var jsonPathEvaluator =
                given()
                        .queryParam("lang", "")
                        .when()
                        .get(url)
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .contentType(ContentType.TEXT)
                        .extract()
                        .jsonPath();

        var returnedLocaleCode = jsonPathEvaluator.get("locale");
        assertThat(returnedLocaleCode).isEqualTo(i18nDefaultLocaleCode);
    }

    @Test
    public void getLocalePhrases_GivenI18nDisabledAndInvalidLocaleCode_ReturnsDefaultLocalePhrases() {
        var url = "/WebAPI/i18n";
        var jsonPathEvaluator =
                given()
                        .queryParam("lang", "BAD")
                        .when()
                        .get(url)
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .contentType(ContentType.TEXT)
                        .extract()
                        .jsonPath();

        var returnedLocaleCode = jsonPathEvaluator.get("locale");
        assertThat(returnedLocaleCode).isEqualTo(i18nDefaultLocaleCode);
    }

    @Test
    public void getLocales_GivenI18nDisabled_ReturnsLocaleList() {
        var url = "/WebAPI/i18n/locales";
        var response =
                given()
                        .when()
                        .get(url)
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract()
                        .response();
        var locales = response.as(new TypeRef<List<AtlasLocale>>() {
        });
        assertThat(locales).hasSize(4);

        var en = locales.get(0);
        assertThat(en.code()).isEqualTo("en");
        assertThat(en.name()).isEqualTo("English");
        assertThat(en.isDefault()).isTrue();

        var ru = locales.get(1);
        assertThat(ru.code()).isEqualTo("ru");
        assertThat(ru.name()).isEqualTo("Русский");
        assertThat(ru.isDefault()).isFalse();

        var ko = locales.get(2);
        assertThat(ko.code()).isEqualTo("ko");
        assertThat(ko.name()).isEqualTo("한국어");
        assertThat(ko.isDefault()).isFalse();

        var zh = locales.get(3);
        assertThat(zh.code()).isEqualTo("zh");
        assertThat(zh.name()).isEqualTo("中文");
        assertThat(zh.isDefault()).isFalse();
    }

    private String generateMd5(String input) {
        try {
            var md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available.", e);
        }
    }
}
