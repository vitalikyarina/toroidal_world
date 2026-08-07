package com.toroidalworld.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.toroidalworld.ToroidalWorld;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import net.fabricmc.loader.api.FabricLoader;

// Fabric has no config API, so the client config is one hand-rolled JSON (config/toroidal_world.json) read once at
// first use. The key mirrors the NeoForge toml so the docs describe one name.
public final class FabricWorldLoopConfig {
    private static final String FILE_NAME = ToroidalWorld.MODID + ".json";
    private static final String SHOW_RAW_F3_KEY = "showRawCoordinatesInF3";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Boolean showRawF3Coordinates;

    public static boolean showRawF3Coordinates() {
        if (showRawF3Coordinates == null) {
            showRawF3Coordinates = load();
        }

        return showRawF3Coordinates;
    }

    private static boolean load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                JsonObject config = GSON.fromJson(reader, JsonObject.class);
                if (config != null && config.has(SHOW_RAW_F3_KEY)) {
                    return config.get(SHOW_RAW_F3_KEY).getAsBoolean();
                }
            } catch (IOException | RuntimeException e) {
                ToroidalWorld.LOGGER.warn("Could not read {}, using defaults", file, e);
                return false;
            }
        }

        JsonObject defaults = new JsonObject();
        defaults.addProperty(SHOW_RAW_F3_KEY, false);
        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(defaults, writer);
        } catch (IOException e) {
            ToroidalWorld.LOGGER.warn("Could not write {}", file, e);
        }

        return false;
    }

    private FabricWorldLoopConfig() {
    }
}
