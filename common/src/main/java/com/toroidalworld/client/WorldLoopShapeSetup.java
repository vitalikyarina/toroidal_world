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

// Registers "Toroidal" into the core World Shape row, next to the vanilla World Type. Called by each loader's client
// setup — the row itself is drawn by the mod's own CreateWorldScreen mixins, so registration is the only loader-bound
// moment.
//
// The size lives here as plain screen state, and that is safe for exactly one reason: it is read once, in applyAtCreation,
// while the world is being built — never after. Nothing has to survive a restart, because what survives is the generator
// the shape leaves behind.
public final class WorldLoopShapeSetup {
    private static final int DEFAULT_SIZE_CHUNKS = 32;
    private static final String TOROIDAL_LABEL_KEY = "gui.toroidal_world.world_shape.toroidal";

    private static final WorldLoopBounds DEFAULT_WRAPPING = WorldLoopBounds.ofWidth(DEFAULT_SIZE_CHUNKS);
    private static final WorldLoopBounds DEFAULT_END_WRAPPING =
            WorldLoopBounds.ofWidth(WorldLoopSizes.END_DEFAULT_CHUNK_WIDTH);

    private static WorldLoopBounds wrapping = DEFAULT_WRAPPING;

    // The End wraps at its own width, untied to the overworld: the end portal lands on a fixed platform
    // (coordinateScale is 1.0), so there is no identity like the nether's portal mapping to keep — no divisibility,
    // no clamping, just a second width.
    private static WorldLoopBounds endWrapping = DEFAULT_END_WRAPPING;

    // Not necessarily NetherScales.DEFAULT in the end: the scales a world can offer follow from its width, and the
    // settings screen re-picks seeded with the scale the player last picked by hand (1:8 until they pick one),
    // falling down the chain when the width disallows it. At the default size 1:8 would leave a nether too small to
    // render, so what the player actually opens on is the largest scale the width admits.
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

    // Re-create hands over the source world's dimensions. The shape was never stored, but the overworld generator was:
    // a ShapedChunkGenerator is this shape's mark, and it carries the exact bounds, so the size seeds straight off it.
    // A generator of any other class means this world was not looped — not ours to claim. Neither is a shaped world
    // whose bounds the screen cannot represent (hand-edited save data): claiming it would either throw on the partial
    // chunkWidth() or silently re-create a world of a different shape, so it declines instead and the screen opens on
    // the default.
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

    // The End width is stored the same way the overworld's is — on its own looped generator. A world whose End is not
    // looped (created before the End wrapped) or not square (hand-edited) has no width the screen could show, so the
    // default stands in.
    private static WorldLoopBounds readEndWrapping(WorldDimensions dimensions) {
        LevelStem end = dimensions.get(LevelStem.END).orElse(null);
        if (end != null && end.generator() instanceof ShapedChunkGenerator endShaped) {
            WorldLoopBounds endBounds = endShaped.wrapping();
            return endBounds.isSquare() ? endBounds : DEFAULT_END_WRAPPING;
        }

        return DEFAULT_END_WRAPPING;
    }

    // The claim guard: one instanceof against the wrapping interface, then the shape check. Only a square looped world
    // is claimable — the single size field on the settings screen describes no other bounds.
    private static @Nullable WorldLoopBounds loopedWrappingOf(ChunkGenerator overworld) {
        if (overworld instanceof ShapedChunkGenerator shaped && shaped.wrapping().isSquare()) {
            return shaped.wrapping();
        }

        return null;
    }

    // The scale is not stored either; it is the ratio the two widths already carry — overworldWidth / netherWidth — the
    // same identity withLoopedNether created them with. A nether that is not itself looped (a world created without one,
    // or shaped before the nether wrapped) or not square (hand-edited) has no ratio to read, so the default stands in
    // and normalize pulls it to a pair this width actually allows.
    private static int readNetherScale(WorldDimensions dimensions, int overworldChunkWidth) {
        LevelStem nether = dimensions.get(LevelStem.NETHER).orElse(null);
        if (nether != null && nether.generator() instanceof ShapedChunkGenerator netherShaped
                && netherShaped.wrapping().isSquare()) {
            return overworldChunkWidth / netherShaped.wrapping().chunkWidth();
        }

        return NetherScales.DEFAULT;
    }

    // The chosen world type has already built the dimensions; the shape adopts whatever generator it produced and wraps
    // it in the matching looped generator, so every world type keeps its own terrain and merely gains a boundary that
    // wraps: the noise types (Default / Large Biomes / Amplified / Single Biome) through LoopedChunkGenerator, Superflat
    // through LoopedFlatChunkGenerator. A world type that is neither has no looped counterpart and is left untouched.
    private static WorldDimensions applyAtCreation(RegistryAccess.Frozen registries, WorldDimensions dimensions) {
        WorldDimensions withLoopedOverworld = withLoopedDimension(dimensions, LevelStem.OVERWORLD, wrapping);
        if (withLoopedOverworld == dimensions) {
            return dimensions;
        }

        WorldDimensions withLoopedNether = withLoopedDimension(withLoopedOverworld, LevelStem.NETHER, netherWrapping());
        return withLoopedDimension(withLoopedNether, LevelStem.END, endWrapping);
    }

    // The nether wraps too, at overworldWidth / scale — the identity that keeps portal linking well defined on a torus.
    // The scale is normalized here and not only in the settings screen, because a world can be created without that
    // screen ever being opened, and the defaults alone do not have to be a legal pair.
    //
    // Nothing stores the scale: the nether carries its own bounds the way the overworld does, and the scale is always
    // recoverable as one width divided by the other.
    private static WorldLoopBounds netherWrapping() {
        int scale = NetherScales.normalize(netherScale, wrapping.chunkWidth());
        return WorldLoopBounds.ofWidth(NetherScales.netherChunkWidth(wrapping.chunkWidth(), scale));
    }

    // Rebuilds the named stem's generator as the matching looped generator carrying the given bounds. A stem that is
    // missing or has no looped counterpart leaves the dimensions untouched — the same instance, so a caller can read
    // "did it wrap" off identity. Vanilla only offers this rebuild for the overworld (replaceOverworldGenerator); this
    // one path serves every dimension the same way.
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
