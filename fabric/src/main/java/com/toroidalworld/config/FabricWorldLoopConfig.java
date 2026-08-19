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

public final class FabricWorldLoopConfig {
    private static final String FILE_NAME = ToroidalWorld.MODID + ".json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static JsonObject load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                JsonObject config = GSON.fromJson(reader, JsonObject.class);
                if (config != null) {
                    return config;
                }
            } catch (IOException | RuntimeException e) {
                ToroidalWorld.LOGGER.warn("Could not read {}, using defaults", file, e);
                return new JsonObject();
            }
        }

        JsonObject defaults = new JsonObject();
        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(defaults, writer);
        } catch (IOException e) {
            ToroidalWorld.LOGGER.warn("Could not write {}", file, e);
        }

        return defaults;
    }

    private FabricWorldLoopConfig() {
    }
}
