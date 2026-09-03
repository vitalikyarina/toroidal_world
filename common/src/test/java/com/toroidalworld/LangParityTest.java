package com.toroidalworld;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class LangParityTest {
    private static final String REFERENCE_LOCALE = "en_us";
    private static final List<String> LOCALES = List.of(REFERENCE_LOCALE, "uk_ua", "de_de", "es_ar", "es_es", "es_mx",
            "fr_fr", "ja_jp", "ko_kr", "pl_pl", "pt_br", "zh_cn");

    @Test
    void everyLocaleCarriesEveryKeyOfTheReferenceLocale() {
        Set<String> reference = read(REFERENCE_LOCALE).keySet();

        for (String locale : LOCALES) {
            assertEquals(reference, read(locale).keySet(), "in " + locale);
        }
    }

    private static JsonObject read(String locale) {
        String path = "/assets/" + ToroidalWorld.MODID + "/lang/" + locale + ".json";
        try (InputStream stream = LangParityTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception failure) {
            throw new AssertionError(path, failure);
        }
    }
}
