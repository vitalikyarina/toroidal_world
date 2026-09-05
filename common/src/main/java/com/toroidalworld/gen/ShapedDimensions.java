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
import com.toroidalworld.platform.Platforms;
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

        ChunkGenerator rebuilt = shapedGeneratorFor(stem.generator(), shape);
        ChunkGenerator marked = rebuilt != null ? rebuilt : stampedGeneratorFor(stem.generator(), shape);
        if (marked == null) {
            return dimensions;
        }

        Map<ResourceKey<LevelStem>, LevelStem> stems = new HashMap<>(dimensions.dimensions());
        stems.put(key, Platforms.get().withGenerator(stem, marked));
        return new WorldDimensions(stems);
    }

    public static WorldDimensions stripShapes(WorldDimensions dimensions) {
        Map<ResourceKey<LevelStem>, LevelStem> stripped = null;
        for (Map.Entry<ResourceKey<LevelStem>, LevelStem> entry : dimensions.dimensions().entrySet()) {
            LevelStem stem = entry.getValue();
            if (stem.generator() instanceof ShapeStamp stamp) {
                stamp.toroidal$clearStamp();
            }

            if (!(stem.generator() instanceof ShapedChunkGenerator shaped)) {
                continue;
            }

            if (stripped == null) {
                stripped = new HashMap<>(dimensions.dimensions());
            }

            stripped.put(entry.getKey(), Platforms.get().withGenerator(stem, shaped.unshaped()));
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

            ChunkGenerator rebuilt = shapedGeneratorFor(datapackStem.generator(), shape);
            if (rebuilt != null) {
                restored.put(entry.key(), Platforms.get().withGenerator(datapackStem, rebuilt));
                overrides.put(entry.key(), override(Outcome.RESHAPED, datapackStem));
                continue;
            }

            ChunkGenerator stamped = stampedGeneratorFor(datapackStem.generator(), shape);
            restored.put(entry.key(),
                    stamped == null ? storedStem : Platforms.get().withGenerator(datapackStem, stamped));
            overrides.put(entry.key(), override(stamped == null ? Outcome.REFUSED : Outcome.STAMPED, datapackStem));
        }

        DatapackStemOverrides.replaceAll(overrides);
        return restored.isEmpty() ? datapackDimensions : withStems(datapackDimensions, restored);
    }

    private static StemOverride override(Outcome outcome, LevelStem datapackStem) {
        return new StemOverride(outcome, datapackStem.generator().getClass().getSimpleName());
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

        FlatShape worldShape = ShapedChunkGenerator.wrappedShapeOf(overworld.generator());
        if (worldShape == null || worldShape.skewChunks() != FlatShape.NO_SKEW || worldShape.mirror() != null) {
            return;
        }

        double overworldScale = overworld.type().value().coordinateScale();
        dimensions.holders().forEach(entry -> {
            LevelStem stem = entry.value();
            if (entry.key() == LevelStem.OVERWORLD
                    || carriesShape(stem.generator())
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
        return stem == null ? null : ShapedChunkGenerator.wrappedShapeOf(stem.generator());
    }

    public static boolean canTakeShape(WorldDimensions dimensions) {
        LevelStem overworld = dimensions.get(LevelStem.OVERWORLD).orElse(null);
        return overworld != null && canTakeShape(overworld.generator());
    }

    public static boolean canTakeShape(ChunkGenerator generator) {
        ChunkGenerator base = baseOf(generator);
        return isRebuildable(base) || isStampable(base);
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
        ChunkGenerator base = baseOf(generator);
        if (!isRebuildable(base)) {
            return null;
        }

        return base instanceof NoiseBasedChunkGenerator noise
                ? new LoopedChunkGenerator(noise.getBiomeSource(), noise.generatorSettings(), shape)
                : new LoopedFlatChunkGenerator(((FlatLevelSource) base).settings(), shape);
    }

    private static @Nullable ChunkGenerator stampedGeneratorFor(ChunkGenerator generator, FlatShape shape) {
        ChunkGenerator base = baseOf(generator);
        if (!isStampable(base)) {
            return null;
        }

        ((ShapeStamp) base).toroidal$stamp(shape);
        return base;
    }

    private static boolean carriesShape(ChunkGenerator generator) {
        return generator instanceof ShapedChunkGenerator
                || (generator instanceof ShapeStamp stamp && stamp.toroidal$stampedShape() != null);
    }

    private static ChunkGenerator baseOf(ChunkGenerator generator) {
        return generator instanceof ShapedChunkGenerator shaped ? shaped.unshaped() : generator;
    }

    private static boolean isRebuildable(ChunkGenerator base) {
        return base.getClass() == NoiseBasedChunkGenerator.class || base.getClass() == FlatLevelSource.class;
    }

    private static boolean isStampable(ChunkGenerator base) {
        return base instanceof NoiseBasedChunkGenerator && base instanceof ShapeStamp;
    }

    private ShapedDimensions() {
    }
}
