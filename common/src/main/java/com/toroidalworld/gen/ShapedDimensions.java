package com.toroidalworld.gen;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.shape.FlatShape;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class ShapedDimensions {

    public static WorldDimensions withShape(WorldDimensions dimensions, ResourceKey<LevelStem> key, FlatShape shape) {
        LevelStem stem = dimensions.get(key).orElse(null);
        if (stem == null) {
            return dimensions;
        }

        ChunkGenerator shaped = shapedGeneratorFor(stem.generator(), shape);
        if (shaped == null) {
            return dimensions;
        }

        Map<ResourceKey<LevelStem>, LevelStem> stems = new HashMap<>(dimensions.dimensions());
        stems.put(key, new LevelStem(stem.type(), shaped));
        return new WorldDimensions(stems);
    }

    public static WorldDimensions stripShapes(WorldDimensions dimensions) {
        Map<ResourceKey<LevelStem>, LevelStem> stripped = null;
        for (Map.Entry<ResourceKey<LevelStem>, LevelStem> entry : dimensions.dimensions().entrySet()) {
            LevelStem stem = entry.getValue();
            if (!(stem.generator() instanceof ShapedChunkGenerator shaped)) {
                continue;
            }

            if (stripped == null) {
                stripped = new HashMap<>(dimensions.dimensions());
            }

            stripped.put(entry.getKey(), new LevelStem(stem.type(), shaped.unshaped()));
        }

        return stripped == null ? dimensions : new WorldDimensions(stripped);
    }

    public static @Nullable FlatShape shapeOf(WorldDimensions dimensions, ResourceKey<LevelStem> key) {
        LevelStem stem = dimensions.get(key).orElse(null);
        return stem != null && stem.generator() instanceof ShapedChunkGenerator shaped ? shaped.shape() : null;
    }

    private static @Nullable ChunkGenerator shapedGeneratorFor(ChunkGenerator generator, FlatShape shape) {
        ChunkGenerator base = generator instanceof ShapedChunkGenerator shaped ? shaped.unshaped() : generator;

        if (base instanceof NoiseBasedChunkGenerator noise) {
            return new LoopedChunkGenerator(noise.getBiomeSource(), noise.generatorSettings(), shape);
        }

        if (base instanceof FlatLevelSource flat) {
            return new LoopedFlatChunkGenerator(flat.settings(), shape);
        }

        return null;
    }

    private ShapedDimensions() {
    }
}
