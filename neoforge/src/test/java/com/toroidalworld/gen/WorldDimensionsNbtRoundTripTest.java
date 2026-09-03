package com.toroidalworld.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.shape.torus.TorusDimensions;
import com.toroidalworld.shape.torus.TorusSettings;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

@Timeout(60)
class WorldDimensionsNbtRoundTripTest {
    private static final int OVERWORLD_CHUNK_WIDTH = 128;
    private static final int NETHER_SCALE = 8;
    private static final int END_CHUNK_WIDTH = 256;

    private static HolderLookup.Provider worldgen;

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        worldgen = VanillaRegistries.createLookup();
    }

    @Test
    void theUnshapedPresetComesBackUnshaped() {
        WorldDimensions preset = normalWorldDimensions();

        assertNull(ShapedDimensions.shapeOf(rereadThroughNbt(preset), LevelStem.OVERWORLD));
    }

    @Test
    void everyShapedStemComesBackWithTheSameShape() {
        WorldDimensions shaped = shapedPreset();
        assertNotNull(ShapedDimensions.shapeOf(shaped, LevelStem.OVERWORLD), "the fixture carries no shape to lose");

        WorldDimensions reread = rereadThroughNbt(shaped);

        assertEquals(ShapedDimensions.shapeOf(shaped, LevelStem.OVERWORLD),
                ShapedDimensions.shapeOf(reread, LevelStem.OVERWORLD));
        assertEquals(ShapedDimensions.shapeOf(shaped, LevelStem.NETHER),
                ShapedDimensions.shapeOf(reread, LevelStem.NETHER));
        assertEquals(ShapedDimensions.shapeOf(shaped, LevelStem.END),
                ShapedDimensions.shapeOf(reread, LevelStem.END));
    }

    private static WorldDimensions shapedPreset() {
        TorusSettings settings = new TorusSettings(WorldLoopBounds.ofWidth(OVERWORLD_CHUNK_WIDTH), NETHER_SCALE,
                WorldLoopBounds.ofWidth(END_CHUNK_WIDTH));
        return TorusDimensions.apply(normalWorldDimensions(), settings);
    }

    // 1.21.1's WorldPresets.createNormalWorldDimensions takes a RegistryAccess; the lookup this harness bootstraps
    // is a HolderLookup.Provider, so the preset is read off it directly — the body 26.x ships under that name.
    private static WorldDimensions normalWorldDimensions() {
        return worldgen.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL)
                .value()
                .createWorldDimensions();
    }

    private static WorldDimensions rereadThroughNbt(WorldDimensions dimensions) {
        RegistryOps<Tag> ops = worldgen.createSerializationContext(NbtOps.INSTANCE);
        Tag written = WorldDimensions.CODEC.encoder().encodeStart(ops, dimensions).getOrThrow();
        return WorldDimensions.CODEC.decoder().parse(ops, written).getOrThrow();
    }
}
