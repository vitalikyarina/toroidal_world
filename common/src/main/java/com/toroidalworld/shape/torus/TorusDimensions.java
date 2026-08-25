package com.toroidalworld.shape.torus;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.gen.LoopedChunkGenerator;
import com.toroidalworld.gen.LoopedFlatChunkGenerator;
import com.toroidalworld.gen.ShapedChunkGenerator;
import com.toroidalworld.options.NetherScales;
import com.toroidalworld.options.WorldLoopBounds;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class TorusDimensions {

    public static WorldDimensions apply(WorldDimensions dimensions, TorusSettings settings) {
        WorldDimensions withLoopedOverworld =
                withLoopedDimension(dimensions, LevelStem.OVERWORLD, settings.overworld());
        if (withLoopedOverworld == dimensions) {
            return dimensions;
        }

        WorldDimensions withLoopedNether =
                withLoopedDimension(withLoopedOverworld, LevelStem.NETHER, netherWrapping(settings));
        return withLoopedDimension(withLoopedNether, LevelStem.END, settings.end());
    }

    public static @Nullable TorusSettings read(WorldDimensions dimensions) {
        WorldLoopBounds overworld = loopedWrappingOf(dimensions.overworld());
        if (overworld == null) {
            return null;
        }

        int overworldChunkWidth = overworld.chunkWidth();
        return new TorusSettings(
                overworld,
                NetherScales.normalize(readNetherScale(dimensions, overworldChunkWidth), overworldChunkWidth),
                readEndWrapping(dimensions));
    }

    private static WorldLoopBounds netherWrapping(TorusSettings settings) {
        int overworldChunkWidth = settings.overworld().chunkWidth();
        int scale = NetherScales.normalize(settings.netherScale(), overworldChunkWidth);
        return WorldLoopBounds.ofWidth(NetherScales.netherChunkWidth(overworldChunkWidth, scale));
    }

    private static WorldLoopBounds readEndWrapping(WorldDimensions dimensions) {
        LevelStem end = dimensions.get(LevelStem.END).orElse(null);
        if (end != null && end.generator() instanceof ShapedChunkGenerator endShaped) {
            WorldLoopBounds endBounds = endShaped.wrapping();
            return endBounds.isSquare() ? endBounds : TorusSettings.DEFAULT.end();
        }

        return TorusSettings.DEFAULT.end();
    }

    private static @Nullable WorldLoopBounds loopedWrappingOf(ChunkGenerator overworld) {
        if (overworld instanceof ShapedChunkGenerator shaped && shaped.wrapping().isSquare()) {
            return shaped.wrapping();
        }

        return null;
    }

    private static int readNetherScale(WorldDimensions dimensions, int overworldChunkWidth) {
        LevelStem nether = dimensions.get(LevelStem.NETHER).orElse(null);
        if (nether != null && nether.generator() instanceof ShapedChunkGenerator netherShaped
                && netherShaped.wrapping().isSquare()) {
            return overworldChunkWidth / netherShaped.wrapping().chunkWidth();
        }

        return NetherScales.DEFAULT;
    }

    private static WorldDimensions withLoopedDimension(WorldDimensions dimensions, ResourceKey<LevelStem> key,
            WorldLoopBounds wrapping) {
        LevelStem stem = dimensions.get(key).orElse(null);
        if (stem == null) {
            return dimensions;
        }

        ChunkGenerator looped = loopedGeneratorFor(stem.generator(), wrapping);
        if (looped == null) {
            return dimensions;
        }

        Map<ResourceKey<LevelStem>, LevelStem> stems = new HashMap<>(dimensions.dimensions());
        stems.put(key, new LevelStem(stem.type(), looped));
        return new WorldDimensions(stems);
    }

    private static @Nullable ChunkGenerator loopedGeneratorFor(ChunkGenerator generator, WorldLoopBounds wrapping) {
        if (generator instanceof NoiseBasedChunkGenerator noise) {
            return new LoopedChunkGenerator(noise.getBiomeSource(), noise.generatorSettings(), wrapping);
        }

        if (generator instanceof FlatLevelSource flat) {
            return new LoopedFlatChunkGenerator(flat.settings(), wrapping);
        }

        return null;
    }

    private TorusDimensions() {
    }
}
