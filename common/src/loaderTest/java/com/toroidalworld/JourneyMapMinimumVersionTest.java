package com.toroidalworld;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Timeout(60)
class JourneyMapMinimumVersionTest {
    private static final String NEOFORGE_METADATA = "META-INF/neoforge.mods.toml";
    private static final String FABRIC_METADATA = "fabric.mod.json";
    private static final String JOURNEYMAP_ID = "journeymap";

    private static final Pattern MOD_ID_ENTRY = Pattern.compile("modId\\s*=\\s*\"" + ToroidalWorld.MODID + "\"");
    private static final Pattern DEPENDENCY_BLOCK = Pattern.compile(
            "\\[\\[dependencies\\.[^\\]]+\\]\\](.*?)(?=\\[\\[|\\z)", Pattern.DOTALL);
    private static final Pattern TOML_ENTRY = Pattern.compile("(\\w+)\\s*=\\s*\"([^\"]*)\"");
    private static final Pattern LOWER_BOUND = Pattern.compile("\\[([^,]+),\\)");

    private static final String ID_KEY = "id";
    private static final String BREAKS_KEY = "breaks";
    private static final String MOD_ID_KEY = "modId";
    private static final String TYPE_KEY = "type";
    private static final String SIDE_KEY = "side";
    private static final String RANGE_KEY = "versionRange";

    private static final String OPTIONAL_TYPE = "optional";
    private static final String CLIENT_SIDE = "CLIENT";
    private static final String BELOW = "<";
    private static final String GAME_LINE_SEPARATOR = "-";

    private record Minimum(String metadata, String version) {
    }

    @Test
    void theMetadataOnTheClasspathRefusesAJourneyMapBelowTheMinimum() throws IOException {
        Optional<Map<String, String>> neoforge = neoForgeDependency();
        Optional<String> fabric = fabricBreak();

        assertTrue(neoforge.isPresent() || fabric.isPresent(), "no " + ToroidalWorld.MODID
                + " metadata on the classpath declares a " + JOURNEYMAP_ID + " version constraint");

        if (neoforge.isPresent()) {
            Map<String, String> dependency = neoforge.get();
            assertEquals(OPTIONAL_TYPE, dependency.get(TYPE_KEY),
                    NEOFORGE_METADATA + " must refuse an out-of-range " + JOURNEYMAP_ID + ", which only "
                            + OPTIONAL_TYPE + " does");
            assertEquals(CLIENT_SIDE, dependency.get(SIDE_KEY),
                    NEOFORGE_METADATA + " must bind the " + JOURNEYMAP_ID
                            + " constraint to the side its mixins are declared on");
            assertTrue(LOWER_BOUND.matcher(dependency.get(RANGE_KEY)).matches(), NEOFORGE_METADATA + " declares "
                    + JOURNEYMAP_ID + " range " + dependency.get(RANGE_KEY) + ", which names no open lower bound");
        }
        if (fabric.isPresent()) {
            assertTrue(fabric.get().startsWith(BELOW), FABRIC_METADATA + " declares " + JOURNEYMAP_ID + " break "
                    + fabric.get() + ", which does not read as a lower bound");
        }
    }

    @Test
    void everyDeclaredMinimumNamesItsGameLine() throws IOException {
        for (Minimum minimum : declaredMinimums()) {
            assertTrue(minimum.version().contains(GAME_LINE_SEPARATOR),
                    minimum.metadata() + " declares " + JOURNEYMAP_ID + " minimum " + minimum.version()
                            + ", but JourneyMap versions read <game line>-<build>, so a bare build number"
                            + " compares against the wrong half");
        }
    }

    private static List<Minimum> declaredMinimums() throws IOException {
        List<Minimum> minimums = new ArrayList<>();
        neoForgeDependency().ifPresent(dependency -> {
            Matcher bound = LOWER_BOUND.matcher(dependency.getOrDefault(RANGE_KEY, ""));
            if (bound.matches()) {
                minimums.add(new Minimum(NEOFORGE_METADATA, bound.group(1)));
            }
        });
        fabricBreak().ifPresent(predicate -> {
            if (predicate.startsWith(BELOW)) {
                minimums.add(new Minimum(FABRIC_METADATA, predicate.substring(BELOW.length())));
            }
        });
        return List.copyOf(minimums);
    }

    private static Optional<Map<String, String>> neoForgeDependency() throws IOException {
        for (URL metadata : resources(NEOFORGE_METADATA)) {
            String toml = read(metadata);
            if (!MOD_ID_ENTRY.matcher(toml).find()) {
                continue;
            }
            Matcher blocks = DEPENDENCY_BLOCK.matcher(toml);
            while (blocks.find()) {
                Map<String, String> entries = new HashMap<>();
                Matcher entry = TOML_ENTRY.matcher(blocks.group(1));
                while (entry.find()) {
                    entries.put(entry.group(1), entry.group(2));
                }
                if (JOURNEYMAP_ID.equals(entries.get(MOD_ID_KEY))) {
                    return Optional.of(entries);
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<String> fabricBreak() throws IOException {
        for (URL metadata : resources(FABRIC_METADATA)) {
            JsonObject json = JsonParser.parseString(read(metadata)).getAsJsonObject();
            JsonElement id = json.get(ID_KEY);
            if (id == null || !ToroidalWorld.MODID.equals(id.getAsString())) {
                continue;
            }
            JsonObject breaks = json.getAsJsonObject(BREAKS_KEY);
            if (breaks != null && breaks.has(JOURNEYMAP_ID)) {
                return Optional.of(breaks.get(JOURNEYMAP_ID).getAsString());
            }
        }
        return Optional.empty();
    }

    private static List<URL> resources(String name) throws IOException {
        return Collections.list(loader().getResources(name));
    }

    private static String read(URL url) throws IOException {
        try (InputStream stream = url.openStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static ClassLoader loader() {
        return JourneyMapMinimumVersionTest.class.getClassLoader();
    }
}
