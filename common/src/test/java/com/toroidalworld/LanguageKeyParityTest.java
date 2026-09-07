package com.toroidalworld;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class LanguageKeyParityTest {
    private static final String LANG_RESOURCE_DIR = "/assets/toroidal_world/lang";
    private static final String REFERENCE_LOCALE = "en_us.json";
    private static final String JSON_SUFFIX = ".json";

    @Test
    void everyLocaleStatesExactlyTheKeysOfTheReference() throws IOException, URISyntaxException {
        List<Path> locales = localeFiles();
        Path reference = locales.stream().filter(locale -> fileName(locale).equals(REFERENCE_LOCALE)).findFirst()
                .orElseThrow(() -> new AssertionError(LANG_RESOURCE_DIR + ": no " + REFERENCE_LOCALE));
        assertTrue(locales.size() > 1, LANG_RESOURCE_DIR + ": " + REFERENCE_LOCALE + " ships alone");

        Set<String> expected = keysOf(reference);
        assertFalse(expected.isEmpty(), REFERENCE_LOCALE + ": no keys");

        for (Path locale : locales) {
            if (locale.equals(reference)) {
                continue;
            }

            Set<String> stated = keysOf(locale);
            Set<String> missing = new TreeSet<>(expected);
            missing.removeAll(stated);
            Set<String> extra = new TreeSet<>(stated);
            extra.removeAll(expected);
            assertTrue(missing.isEmpty() && extra.isEmpty(),
                    fileName(locale) + ": missing " + missing + ", extra " + extra);
        }
    }

    private static List<Path> localeFiles() throws IOException, URISyntaxException {
        URL directory = LanguageKeyParityTest.class.getResource(LANG_RESOURCE_DIR);
        assertNotNull(directory, "missing jar resource " + LANG_RESOURCE_DIR);
        try (Stream<Path> files = Files.list(Path.of(directory.toURI()))) {
            return files.filter(file -> fileName(file).endsWith(JSON_SUFFIX)).sorted().toList();
        }
    }

    private static Set<String> keysOf(Path locale) throws IOException {
        try (Reader reader = Files.newBufferedReader(locale, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            return new TreeSet<>(json.keySet());
        }
    }

    private static String fileName(Path locale) {
        return locale.getFileName().toString();
    }
}
