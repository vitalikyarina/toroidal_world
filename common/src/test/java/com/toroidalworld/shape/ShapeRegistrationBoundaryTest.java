package com.toroidalworld.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
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
import com.toroidalworld.shape.FlatShape;

import io.netty.buffer.Unpooled;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldDimensions;

class ShapeRegistrationBoundaryTest {
    private static final RegistryAccess.Frozen REGISTRIES =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private static final HolderLookup.Provider WORLDGEN = VanillaRegistries.createLookup();

    private static final Identifier CYLINDER_ID =
            Identifier.fromNamespaceAndPath(ToroidalWorld.MODID, "boundary_test_cylinder");

    private static final FlatShape CYLINDER = FlatShape.cylinder(
            new WorldLoopBounds(new AxisBounds.Looped(-16, 16), AxisBounds.Unbounded.INSTANCE));

    private static final FlatShape WIDER_CYLINDER = FlatShape.cylinder(
            new WorldLoopBounds(new AxisBounds.Looped(-64, 64), AxisBounds.Unbounded.INSTANCE));

    private static WorldShape cylinder;

    private static boolean settingsWereReset;

    @BeforeAll
    static void registerTheShapeThroughThePublicSeamsAlone() {
        cylinder = WorldShape.of(
                CYLINDER_ID,
                Component.literal("Boundary test cylinder"),
                Component.literal("A cylinder registered by the boundary test"),
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

    private static WorldDimensions vanillaOverworldAndNether() {
        Map<ResourceKey<LevelStem>, LevelStem> stems = new HashMap<>(vanillaOverworld().dimensions());
        stems.put(LevelStem.NETHER, new LevelStem(
                WORLDGEN.lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(BuiltinDimensionTypes.NETHER),
                new NoiseBasedChunkGenerator(
                        new FixedBiomeSource(WORLDGEN.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.NETHER_WASTES)),
                        WORLDGEN.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.NETHER))));
        return new WorldDimensions(stems);
    }

    private static NoiseBasedChunkGenerator overworldGenerator(WorldDimensions dimensions) {
        return (NoiseBasedChunkGenerator) dimensions.dimensions().get(LevelStem.OVERWORLD).generator();
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
    void reCreatingWithNormalSelectedDropsTheInheritedShape() {
        WorldDimensions inherited = cylinder.atCreation().apply(REGISTRIES, vanillaOverworld());
        NoiseBasedChunkGenerator vanilla = overworldGenerator(vanillaOverworld());

        WorldShapes.select(WorldShapes.NORMAL);
        WorldDimensions created = WorldShapes.applyAtCreation(REGISTRIES, inherited);

        ChunkGenerator generator = created.dimensions().get(LevelStem.OVERWORLD).generator();
        assertNull(ShapedDimensions.shapeOf(created, LevelStem.OVERWORLD));
        assertFalse(generator instanceof ShapedChunkGenerator, generator.getClass().getName());
        assertEquals(vanilla.generatorSettings(), ((NoiseBasedChunkGenerator) generator).generatorSettings());
    }

    @Test
    void reCreatingIntoAShapeLeavesNoStemTheNewShapeDoesNotWrite() {
        WorldDimensions inherited = ShapedDimensions.withShape(
                ShapedDimensions.withShape(vanillaOverworldAndNether(), LevelStem.OVERWORLD, CYLINDER),
                LevelStem.NETHER, CYLINDER);

        WorldShapes.select(cylinder);
        WorldDimensions created = WorldShapes.applyAtCreation(REGISTRIES, inherited);

        assertEquals(CYLINDER, ShapedDimensions.shapeOf(created, LevelStem.OVERWORLD));
        assertNull(ShapedDimensions.shapeOf(created, LevelStem.NETHER));
    }

    @Test
    void reCreatingFromOneShapeIntoAnotherTakesTheNewGeometry() {
        WorldDimensions inherited =
                ShapedDimensions.withShape(vanillaOverworld(), LevelStem.OVERWORLD, WIDER_CYLINDER);

        WorldShapes.select(cylinder);
        WorldDimensions created = WorldShapes.applyAtCreation(REGISTRIES, inherited);

        assertEquals(CYLINDER, ShapedDimensions.shapeOf(created, LevelStem.OVERWORLD));
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
        WrappingSettingsPayload.STREAM_CODEC.encode(buffer, new WrappingSettingsPayload(Level.OVERWORLD, CYLINDER));
        assertEquals(CYLINDER, WrappingSettingsPayload.STREAM_CODEC.decode(buffer).shape());
        assertEquals(0, buffer.readableBytes());

        assertTrue(WorldFolds.of(CYLINDER).isWrapped());
        assertTrue(WorldFolds.of(CYLINDER).chunkDomain(Direction.Axis.X).isOver(64));
        assertTrue(!WorldFolds.of(CYLINDER).chunkDomain(Direction.Axis.Z).isOver(1_000_000));
    }
}
