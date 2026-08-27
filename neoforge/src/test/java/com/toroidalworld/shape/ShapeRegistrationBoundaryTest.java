package com.toroidalworld.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.toroidalworld.ToroidalWorld;
import com.toroidalworld.client.shape.ShapeCustomizers;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.gen.ShapedChunkGenerator;
import com.toroidalworld.gen.ShapedDimensions;
import com.toroidalworld.net.WrappingSettingsPayload;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;

import io.netty.buffer.Unpooled;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldDimensions;

class ShapeRegistrationBoundaryTest {
    private static final RegistryAccess.Frozen REGISTRIES =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private static final HolderLookup.Provider WORLDGEN = VanillaRegistries.createLookup();

    private static final ResourceLocation CYLINDER_ID =
            ResourceLocation.fromNamespaceAndPath(ToroidalWorld.MODID, "boundary_test_cylinder");

    private static final FlatShape CYLINDER = FlatShape.cylinder(
            new WorldLoopBounds(new AxisBounds.Looped(-16, 16), AxisBounds.Unbounded.INSTANCE));

    private static WorldShape cylinder;

    private static boolean settingsWereReset;

    @BeforeAll
    static void registerTheShapeThroughThePublicSeamsAlone() {
        cylinder = WorldShape.of(
                CYLINDER_ID,
                Component.literal("Boundary test cylinder"),
                (registries, dimensions) -> ShapedDimensions.withShape(dimensions, LevelStem.OVERWORLD, CYLINDER),
                () -> settingsWereReset = true,
                (registries, dimensions) ->
                        CYLINDER.equals(ShapedDimensions.shapeOf(dimensions, LevelStem.OVERWORLD)));

        WorldShapes.register(cylinder);
        ShapeCustomizers.register(CYLINDER_ID, parent -> parent);
    }

    @AfterEach
    void leaveTheRegistryAsItWasFound() {
        WorldShapes.select(WorldShapes.NORMAL);
        settingsWereReset = false;
    }

    private static WorldDimensions vanillaOverworld() {
        NoiseBasedChunkGenerator generator = new NoiseBasedChunkGenerator(
                new FixedBiomeSource(WORLDGEN.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS)),
                WORLDGEN.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD));

        return new WorldDimensions(Map.of(LevelStem.OVERWORLD, new LevelStem(
                WORLDGEN.lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(BuiltinDimensionTypes.OVERWORLD),
                generator)));
    }

    @Test
    void theShapeIsOfferedAfterNormal() {
        List<WorldShape> offered = WorldShapes.shapes();

        assertSame(WorldShapes.NORMAL, offered.get(0));
        assertTrue(offered.contains(cylinder), offered.toString());
    }

    @Test
    void creatingAWorldThroughTheRegistryLeavesTheShapeInTheGenerator() {
        WorldDimensions plain = vanillaOverworld();
        assertNull(ShapedDimensions.shapeOf(plain, LevelStem.OVERWORLD));

        WorldShapes.select(cylinder);
        WorldDimensions created = WorldShapes.applyAtCreation(REGISTRIES, plain);

        assertEquals(CYLINDER, ShapedDimensions.shapeOf(created, LevelStem.OVERWORLD));
    }

    @Test
    void reCreateAdoptsTheShapeThatClaimsTheWorld() {
        WorldShapes.restoreFromExisting(REGISTRIES, vanillaOverworld());
        assertSame(WorldShapes.NORMAL, WorldShapes.selected());

        WorldDimensions created = cylinder.atCreation().apply(REGISTRIES, vanillaOverworld());
        WorldShapes.restoreFromExisting(REGISTRIES, created);

        assertSame(cylinder, WorldShapes.selected());
    }

    @Test
    void openingTheScreenResetsEveryRegisteredShape() {
        WorldShapes.select(cylinder);

        WorldShapes.resetToDefault();

        assertSame(WorldShapes.NORMAL, WorldShapes.selected());
        assertTrue(settingsWereReset);
    }

    @Test
    void onlyAShapeWithAScreenLightsUpTheCustomizeButton() {
        assertNotNull(ShapeCustomizers.of(cylinder));
        assertNull(ShapeCustomizers.of(WorldShapes.NORMAL));
    }

    @Test
    void theShapeGeometryPassesTheGeneratorFieldTheWireAndTheFold() {
        JsonElement stored = ShapedChunkGenerator.SHAPE_CODEC.encodeStart(JsonOps.INSTANCE, CYLINDER).getOrThrow();
        assertEquals(CYLINDER, ShapedChunkGenerator.SHAPE_CODEC.parse(JsonOps.INSTANCE, stored).getOrThrow(),
                stored.toString());

        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), REGISTRIES);
        WrappingSettingsPayload.STREAM_CODEC.encode(buffer, new WrappingSettingsPayload(CYLINDER));
        assertEquals(CYLINDER, WrappingSettingsPayload.STREAM_CODEC.decode(buffer).shape());
        assertEquals(0, buffer.readableBytes());

        assertTrue(WorldFolds.of(CYLINDER).isWrapped());
        assertTrue(WorldFolds.of(CYLINDER).chunks.x.isOver(64));
        assertTrue(!WorldFolds.of(CYLINDER).chunks.z.isOver(1_000_000));
    }
}
