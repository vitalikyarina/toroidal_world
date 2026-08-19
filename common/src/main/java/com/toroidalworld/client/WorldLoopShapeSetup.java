package com.toroidalworld.client;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.client.screen.WorldLoopSettingsScreen;
import com.toroidalworld.client.shape.WorldShape;
import com.toroidalworld.client.shape.WorldShapes;
import com.toroidalworld.gen.LoopedChunkGenerator;
import com.toroidalworld.gen.LoopedFlatChunkGenerator;
import com.toroidalworld.gen.ShapedChunkGenerator;
import com.toroidalworld.gen.WorldLoopGenerators;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopSizes;
import com.toroidalworld.options.NetherScales;
import com.toroidalworld.ToroidalWorld;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class WorldLoopShapeSetup {
    private static final int DEFAULT_SIZE_CHUNKS = 32;
    private static final String TOROIDAL_LABEL_KEY = "gui.toroidal_world.world_shape.toroidal";

    private static final WorldLoopBounds DEFAULT_WRAPPING = WorldLoopBounds.ofWidth(DEFAULT_SIZE_CHUNKS);
    private static final WorldLoopBounds DEFAULT_END_WRAPPING =
            WorldLoopBounds.ofWidth(WorldLoopSizes.END_DEFAULT_CHUNK_WIDTH);

    private static WorldLoopBounds wrapping = DEFAULT_WRAPPING;

    private static WorldLoopBounds endWrapping = DEFAULT_END_WRAPPING;

    private static int netherScale = NetherScales.DEFAULT;

    public static void register() {
        WorldShapes.register(WorldShape.of(
                ResourceLocation.fromNamespaceAndPath(ToroidalWorld.MODID, WorldLoopGenerators.TOROIDAL_ID),
                Component.translatable(TOROIDAL_LABEL_KEY),
                parent -> new WorldLoopSettingsScreen(parent, wrapping, netherScale, endWrapping,
                        (chosen, chosenScale, chosenEnd) -> {
                            wrapping = chosen;
                            netherScale = chosenScale;
                            endWrapping = chosenEnd;
                        }),
                WorldLoopShapeSetup::applyAtCreation,
                WorldLoopShapeSetup::resetSettings,
                WorldLoopShapeSetup::restoreFromExisting));
    }

    private static void resetSettings() {
        wrapping = DEFAULT_WRAPPING;
        netherScale = NetherScales.DEFAULT;
        endWrapping = DEFAULT_END_WRAPPING;
    }

    private static boolean restoreFromExisting(RegistryAccess.Frozen registries, WorldDimensions dimensions) {
        WorldLoopBounds overworldWrapping = loopedWrappingOf(dimensions.overworld());
        if (overworldWrapping == null) {
            return false;
        }

        wrapping = overworldWrapping;
        netherScale = NetherScales.normalize(readNetherScale(dimensions, wrapping.chunkWidth()), wrapping.chunkWidth());
        endWrapping = readEndWrapping(dimensions);
        return true;
    }

    private static WorldLoopBounds readEndWrapping(WorldDimensions dimensions) {
        LevelStem end = dimensions.get(LevelStem.END).orElse(null);
        if (end != null && end.generator() instanceof ShapedChunkGenerator endShaped) {
            WorldLoopBounds endBounds = endShaped.wrapping();
            return endBounds.isSquare() ? endBounds : DEFAULT_END_WRAPPING;
        }

        return DEFAULT_END_WRAPPING;
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

    private static WorldDimensions applyAtCreation(RegistryAccess.Frozen registries, WorldDimensions dimensions) {
        WorldDimensions withLoopedOverworld = withLoopedDimension(dimensions, LevelStem.OVERWORLD, wrapping);
        if (withLoopedOverworld == dimensions) {
            return dimensions;
        }

        WorldDimensions withLoopedNether = withLoopedDimension(withLoopedOverworld, LevelStem.NETHER, netherWrapping());
        return withLoopedDimension(withLoopedNether, LevelStem.END, endWrapping);
    }

    private static WorldLoopBounds netherWrapping() {
        int scale = NetherScales.normalize(netherScale, wrapping.chunkWidth());
        return WorldLoopBounds.ofWidth(NetherScales.netherChunkWidth(wrapping.chunkWidth(), scale));
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

    private WorldLoopShapeSetup() {
    }
}
