package com.toroidalworld.gen;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.accessors.ShapeStamp;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.options.WorldLoopSizes;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.Registry;
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

    public static void stampDerived(Registry<LevelStem> dimensions) {
        LevelStem overworld = dimensions.getOptional(LevelStem.OVERWORLD).orElse(null);
        if (overworld == null) {
            return;
        }

        FlatShape worldShape = codecCarriedShapeOf(overworld.generator());
        if (worldShape == null || worldShape.skewChunks() != FlatShape.NO_SKEW || worldShape.mirror() != null) {
            return;
        }

        double overworldScale = overworld.type().value().coordinateScale();
        dimensions.holders().forEach(entry -> {
            LevelStem stem = entry.value();
            if (entry.key() == LevelStem.OVERWORLD
                    || stem.generator() instanceof ShapedChunkGenerator
                    || !(stem.generator() instanceof ShapeStamp stamp)) {
                return;
            }

            FlatShape derived = derivedShape(worldShape, overworldScale, stem.type().value().coordinateScale());
            if (derived != null) {
                stamp.toroidal$stamp(derived);
            }
        });
    }

    public static @Nullable FlatShape shapeOf(WorldDimensions dimensions, ResourceKey<LevelStem> key) {
        LevelStem stem = dimensions.get(key).orElse(null);
        return stem != null && stem.generator() instanceof ShapedChunkGenerator shaped ? shaped.shape() : null;
    }

    private static @Nullable FlatShape codecCarriedShapeOf(ChunkGenerator generator) {
        if (!(generator instanceof ShapedChunkGenerator shaped)) {
            return null;
        }

        return shaped.transformer().isWrapped() ? shaped.shape() : null;
    }

    public static @Nullable FlatShape derivedShape(FlatShape worldShape, double overworldScale, double scale) {
        if (overworldScale <= 0.0 || scale <= 0.0) {
            return null;
        }

        AxisBounds x = derivedAxis(worldShape.bounds().x(), overworldScale, scale);
        AxisBounds z = derivedAxis(worldShape.bounds().z(), overworldScale, scale);
        return x == null || z == null ? null : new FlatShape(new WorldLoopBounds(x, z), FlatShape.NO_SKEW, null);
    }

    private static @Nullable AxisBounds derivedAxis(AxisBounds axis, double overworldScale, double scale) {
        if (!(axis instanceof AxisBounds.Looped looped)) {
            return axis;
        }

        double derived = looped.chunkWidth() * overworldScale / scale;
        int chunkWidth = (int) derived;
        return derived == chunkWidth && WorldLoopSizes.isInRange(chunkWidth)
                ? AxisBounds.Looped.ofWidth(chunkWidth)
                : null;
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
