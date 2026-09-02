package com.toroidalworld;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

final class MixinConfigFixture {
    private static final String NEOFORGE_METADATA = "META-INF/neoforge.mods.toml";
    private static final String FABRIC_METADATA = "fabric.mod.json";
    private static final Pattern MOD_ID_ENTRY = Pattern.compile("modId\\s*=\\s*\"" + ToroidalWorld.MODID + "\"");
    private static final Pattern CONFIG_ENTRY = Pattern.compile("config\\s*=\\s*\"([^\"]+)\"");

    private static final String MIXINS_KEY = "mixins";
    private static final String CONFIG_KEY = "config";
    private static final String ID_KEY = "id";

    static List<String> declaredConfigs() throws IOException {
        Set<String> configs = new LinkedHashSet<>();
        for (URL metadata : resources(NEOFORGE_METADATA)) {
            String toml = read(metadata);
            if (!MOD_ID_ENTRY.matcher(toml).find()) {
                continue;
            }
            Matcher entries = CONFIG_ENTRY.matcher(toml);
            while (entries.find()) {
                configs.add(entries.group(1));
            }
        }
        for (URL metadata : resources(FABRIC_METADATA)) {
            JsonObject json = JsonParser.parseString(read(metadata)).getAsJsonObject();
            if (!ToroidalWorld.MODID.equals(json.get(ID_KEY).getAsString())) {
                continue;
            }
            for (JsonElement entry : json.getAsJsonArray(MIXINS_KEY)) {
                configs.add(entry.isJsonObject()
                        ? entry.getAsJsonObject().get(CONFIG_KEY).getAsString()
                        : entry.getAsString());
            }
        }
        return List.copyOf(configs);
    }

    static boolean isOnClasspath(String config) {
        return MixinConfigFixture.class.getClassLoader().getResource(config) != null;
    }

    private static List<URL> resources(String name) throws IOException {
        return Collections.list(MixinConfigFixture.class.getClassLoader().getResources(name));
    }

    private static String read(URL url) throws IOException {
        try (InputStream stream = url.openStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private MixinConfigFixture() {
    }
}
