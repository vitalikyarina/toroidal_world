package com.toroidalworld.gen;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.accessors.ShapeStamp;
import com.toroidalworld.gen.DatapackStemOverrides.Outcome;
import com.toroidalworld.gen.DatapackStemOverrides.StemOverride;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.options.WorldLoopSizes;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
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

    public static Registry<LevelStem> restoreStoredShapes(WorldDimensions stored, Registry<LevelStem> datapackDimensions) {
        Map<ResourceKey<LevelStem>, LevelStem> restored = new HashMap<>();
        Map<ResourceKey<LevelStem>, StemOverride> overrides = new HashMap<>();
        for (Holder.Reference<LevelStem> entry : datapackDimensions.holders().toList()) {
            LevelStem datapackStem = entry.value();
            LevelStem storedStem = stored.get(entry.key()).orElse(null);
            if (storedStem == null || datapackStem.generator() instanceof ShapedChunkGenerator) {
                continue;
            }

            FlatShape shape = ShapedChunkGenerator.wrappedShapeOf(storedStem.generator());
            if (shape == null) {
                continue;
            }

            ChunkGenerator shaped = shapedGeneratorFor(datapackStem.generator(), shape);
            restored.put(entry.key(), shaped == null ? storedStem : new LevelStem(datapackStem.type(), shaped));
            overrides.put(entry.key(), new StemOverride(
                    shaped == null ? Outcome.REFUSED : Outcome.RESHAPED,
                    datapackStem.generator().getClass().getSimpleName()));
        }

        DatapackStemOverrides.replaceAll(overrides);
        return restored.isEmpty() ? datapackDimensions : withStems(datapackDimensions, restored);
    }

    private static Registry<LevelStem> withStems(Registry<LevelStem> dimensions,
            Map<ResourceKey<LevelStem>, LevelStem> replacements) {
        WritableRegistry<LevelStem> rebuilt = new MappedRegistry<>(Registries.LEVEL_STEM,
                dimensions.registryLifecycle());
        dimensions.holders().forEach(entry -> rebuilt.register(
                entry.key(),
                replacements.getOrDefault(entry.key(), entry.value()),
                dimensions.registrationInfo(entry.key()).orElse(RegistrationInfo.BUILT_IN)));
        return rebuilt.freeze();
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

        if (base instanceof NoiseBasedChunkGenerator noise && noise.getClass() == NoiseBasedChunkGenerator.class) {
            return new LoopedChunkGenerator(noise.getBiomeSource(), noise.generatorSettings(), shape);
        }

        if (base instanceof FlatLevelSource flat && flat.getClass() == FlatLevelSource.class) {
            return new LoopedFlatChunkGenerator(flat.settings(), shape);
        }

        return null;
    }

    private ShapedDimensions() {
    }
}
