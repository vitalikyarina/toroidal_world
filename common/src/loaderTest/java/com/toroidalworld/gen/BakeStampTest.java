package com.toroidalworld.gen;

import static com.toroidalworld.gen.BakeStampFixture.OVERWORLD_CHUNK_WIDTH;
import static com.toroidalworld.gen.BakeStampFixture.chunkWidth;
import static com.toroidalworld.gen.BakeStampFixture.datapackRegistry;
import static com.toroidalworld.gen.BakeStampFixture.foreignGenerator;
import static com.toroidalworld.gen.BakeStampFixture.generatorOf;
import static com.toroidalworld.gen.BakeStampFixture.noiseGenerator;
import static com.toroidalworld.gen.BakeStampFixture.noiseSubclassGenerator;
import static com.toroidalworld.gen.BakeStampFixture.selected;
import static com.toroidalworld.gen.BakeStampFixture.shapeOf;
import static com.toroidalworld.gen.BakeStampFixture.shapedGenerator;
import static com.toroidalworld.gen.BakeStampFixture.squareTorus;
import static com.toroidalworld.gen.BakeStampFixture.stamped;
import static com.toroidalworld.gen.BakeStampFixture.stem;
import static com.toroidalworld.gen.BakeStampFixture.stemKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.toroidalworld.shape.FlatShape;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;

@Timeout(60)
class BakeStampTest {
    private static final double SAME_SCALE = 1.0;
    private static final double HALF_SCALE = 0.5;
    private static final double DOUBLE_SCALE = 2.0;
    private static final double TRIPLE_SCALE = 3.0;
    private static final double NETHER_COORDINATE_SCALE = 8.0;

    private static final int SAME_SCALE_CHUNK_WIDTH = 32;
    private static final int DOUBLE_SCALE_CHUNK_WIDTH = 16;
    private static final int HALF_SCALE_CHUNK_WIDTH = 64;

    private static final int CODEC_CARRIED_CHUNK_WIDTH = 64;

    private static final int STORED_OVERWORLD_CHUNK_WIDTH = 256;
    private static final int STORED_NETHER_CHUNK_WIDTH = 64;
    private static final int STORED_END_CHUNK_WIDTH = 192;
    private static final int DATAPACK_DECLARED_CHUNK_WIDTH = 128;

    private static final ResourceKey<LevelStem> FOREIGN = stemKey("foreign");
    private static final ResourceKey<LevelStem> SIBLING = stemKey("sibling");

    private static HolderLookup.Provider worldgen;

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        worldgen = VanillaRegistries.createLookup();
    }

    @Test
    void aForeignStemAtTheSameScaleTakesTheOverworldsWidth() {
        FlatShape shape = bakeForeign(SAME_SCALE, foreignGenerator());

        assertNotNull(shape, "the foreign stem carries no fold");
        assertEquals(SAME_SCALE_CHUNK_WIDTH, chunkWidth(shape, Direction.Axis.X));
        assertEquals(SAME_SCALE_CHUNK_WIDTH, chunkWidth(shape, Direction.Axis.Z));
    }

    @Test
    void aLargerDeclaredScaleNarrowsTheWidth() {
        FlatShape shape = bakeForeign(DOUBLE_SCALE, foreignGenerator());

        assertNotNull(shape, "the foreign stem carries no fold");
        assertEquals(DOUBLE_SCALE_CHUNK_WIDTH, chunkWidth(shape, Direction.Axis.X));
        assertEquals(DOUBLE_SCALE_CHUNK_WIDTH, chunkWidth(shape, Direction.Axis.Z));
    }

    @Test
    void aSmallerDeclaredScaleWidensTheWidth() {
        FlatShape shape = bakeForeign(HALF_SCALE, foreignGenerator());

        assertNotNull(shape, "the foreign stem carries no fold");
        assertEquals(HALF_SCALE_CHUNK_WIDTH, chunkWidth(shape, Direction.Axis.X));
        assertEquals(HALF_SCALE_CHUNK_WIDTH, chunkWidth(shape, Direction.Axis.Z));
    }

    @Test
    void aScaleWithNoWholeChunkWidthIsRefusedWhileItsSiblingIsStamped() {
        Registry<LevelStem> baked = bake(Map.of(
                FOREIGN, stem(TRIPLE_SCALE, foreignGenerator()),
                SIBLING, stem(SAME_SCALE, foreignGenerator())));

        FlatShape sibling = shapeOf(baked, SIBLING);
        assertNotNull(sibling, "the sibling stem carries no fold, so this run says nothing about the refusal");
        assertEquals(SAME_SCALE_CHUNK_WIDTH, chunkWidth(sibling, Direction.Axis.X));
        assertNull(shapeOf(baked, FOREIGN));
    }

    @Test
    void aStemCarryingItsOwnShapeIsLeftAlone() {
        Registry<LevelStem> baked = bake(Map.of(
                FOREIGN, stem(DOUBLE_SCALE, shapedGenerator(squareTorus(CODEC_CARRIED_CHUNK_WIDTH))),
                SIBLING, stem(SAME_SCALE, foreignGenerator())));

        FlatShape sibling = shapeOf(baked, SIBLING);
        assertNotNull(sibling, "the sibling stem carries no fold, so this run says nothing about the codec shape");
        assertEquals(SAME_SCALE_CHUNK_WIDTH, chunkWidth(sibling, Direction.Axis.X));

        FlatShape carried = shapeOf(baked, FOREIGN);
        assertNotNull(carried, "the codec-carried shape was dropped");
        assertEquals(CODEC_CARRIED_CHUNK_WIDTH, chunkWidth(carried, Direction.Axis.X));
    }

    @Test
    void nothingIsStampedWhenTheOverworldDoesNotFold() {
        FlatShape stamped = bakeForeign(SAME_SCALE, foreignGenerator());
        assertNotNull(stamped, "a folded overworld stamped nothing, so this run says nothing about the control");

        Registry<LevelStem> baked = bake(
                Map.of(LevelStem.OVERWORLD, stem(SAME_SCALE, foreignGenerator())),
                Map.of(FOREIGN, stem(SAME_SCALE, foreignGenerator())));

        assertNull(shapeOf(baked, FOREIGN));
    }

    @Test
    void aDatapackOverworldKeepsTheWorldsStoredShape() {
        Registry<LevelStem> baked = bake(storedToroidalWorld(),
                Map.of(LevelStem.OVERWORLD, stem(SAME_SCALE, noiseGenerator(worldgen))));

        FlatShape shape = shapeOf(baked, LevelStem.OVERWORLD);
        assertNotNull(shape, "the overworld carries no fold");
        assertEquals(STORED_OVERWORLD_CHUNK_WIDTH, chunkWidth(shape, Direction.Axis.X));
        assertEquals(STORED_OVERWORLD_CHUNK_WIDTH, chunkWidth(shape, Direction.Axis.Z));
    }

    @Test
    void aDatapackOverworldKeepsItsOwnBiomesAndSettings() {
        ChunkGenerator datapackOverworld = noiseGenerator(worldgen);
        Registry<LevelStem> baked = bake(storedToroidalWorld(),
                Map.of(LevelStem.OVERWORLD, stem(SAME_SCALE, datapackOverworld)));

        LoopedChunkGenerator restored = assertInstanceOf(LoopedChunkGenerator.class,
                generatorOf(baked, LevelStem.OVERWORLD));
        assertSame(datapackOverworld.getBiomeSource(), restored.getBiomeSource());
        assertSame(((NoiseBasedChunkGenerator) datapackOverworld).generatorSettings(), restored.generatorSettings());
    }

    @Test
    void aDatapackNetherKeepsTheStoredWidthRatherThanTheCoordinateScaleDerivation() {
        Registry<LevelStem> baked = bake(storedToroidalWorld(),
                Map.of(LevelStem.NETHER, stem(NETHER_COORDINATE_SCALE, noiseGenerator(worldgen))));

        FlatShape shape = shapeOf(baked, LevelStem.NETHER);
        assertNotNull(shape, "the nether carries no fold");
        assertEquals(STORED_NETHER_CHUNK_WIDTH, chunkWidth(shape, Direction.Axis.X));
        assertEquals(STORED_NETHER_CHUNK_WIDTH, chunkWidth(shape, Direction.Axis.Z));
    }

    @Test
    void aDatapackEndKeepsTheStoredWidthRatherThanTheOverworlds() {
        Registry<LevelStem> baked = bake(storedToroidalWorld(),
                Map.of(LevelStem.END, stem(SAME_SCALE, noiseGenerator(worldgen))));

        FlatShape shape = shapeOf(baked, LevelStem.END);
        assertNotNull(shape, "the End carries no fold");
        assertEquals(STORED_END_CHUNK_WIDTH, chunkWidth(shape, Direction.Axis.X));
        assertEquals(STORED_END_CHUNK_WIDTH, chunkWidth(shape, Direction.Axis.Z));
    }

    @Test
    void aDatapackGeneratorThatCannotTakeAShapeLeavesTheStoredStemInPlace() {
        Map<ResourceKey<LevelStem>, LevelStem> stored = storedToroidalWorld();
        Registry<LevelStem> baked = bake(stored,
                Map.of(LevelStem.OVERWORLD, stem(SAME_SCALE, foreignGenerator())));

        assertSame(stored.get(LevelStem.OVERWORLD).generator(), generatorOf(baked, LevelStem.OVERWORLD));

        FlatShape shape = shapeOf(baked, LevelStem.OVERWORLD);
        assertNotNull(shape, "the overworld carries no fold");
        assertEquals(STORED_OVERWORLD_CHUNK_WIDTH, chunkWidth(shape, Direction.Axis.X));
    }

    @Test
    void aForeignNoiseSubclassIsKeptOutOfTheRebuildAndLeavesTheStoredStemInPlace() {
        Map<ResourceKey<LevelStem>, LevelStem> stored = storedToroidalWorld();
        Registry<LevelStem> baked = bake(stored,
                Map.of(LevelStem.OVERWORLD, stem(SAME_SCALE, noiseSubclassGenerator(worldgen))));

        assertSame(stored.get(LevelStem.OVERWORLD).generator(), generatorOf(baked, LevelStem.OVERWORLD));

        FlatShape shape = shapeOf(baked, LevelStem.OVERWORLD);
        assertNotNull(shape, "the overworld carries no fold");
        assertEquals(STORED_OVERWORLD_CHUNK_WIDTH, chunkWidth(shape, Direction.Axis.X));
    }

    @Test
    void aDatapackStemDeclaringItsOwnShapeOutranksTheStoredOne() {
        Registry<LevelStem> baked = bake(storedToroidalWorld(), Map.of(LevelStem.OVERWORLD,
                stem(SAME_SCALE, shapedGenerator(squareTorus(DATAPACK_DECLARED_CHUNK_WIDTH)))));

        FlatShape shape = shapeOf(baked, LevelStem.OVERWORLD);
        assertNotNull(shape, "the overworld carries no fold");
        assertEquals(DATAPACK_DECLARED_CHUNK_WIDTH, chunkWidth(shape, Direction.Axis.X));
    }

    @Test
    void aDatapackOverworldStandsWhenTheWorldStoredNoShape() {
        ChunkGenerator datapackOverworld = noiseGenerator(worldgen);
        Registry<LevelStem> baked = bake(
                Map.of(LevelStem.OVERWORLD, stem(SAME_SCALE, foreignGenerator())),
                Map.of(LevelStem.OVERWORLD, stem(SAME_SCALE, datapackOverworld)));

        assertSame(datapackOverworld, generatorOf(baked, LevelStem.OVERWORLD));
        assertNull(shapeOf(baked, LevelStem.OVERWORLD));
    }

    @Test
    void aReshapedStemIsRecordedForTheReportUnderTheDatapacksGeneratorName() {
        bake(storedToroidalWorld(), Map.of(LevelStem.OVERWORLD, stem(SAME_SCALE, noiseGenerator(worldgen))));

        DatapackStemOverrides.StemOverride override = DatapackStemOverrides.of(LevelStem.OVERWORLD);
        assertNotNull(override, "the override went unrecorded");
        assertEquals(DatapackStemOverrides.Outcome.RESHAPED, override.outcome());
        assertEquals(NoiseBasedChunkGenerator.class.getSimpleName(), override.datapackGenerator());
    }

    @Test
    void aRefusedStemIsRecordedForTheReportUnderTheDatapacksGeneratorName() {
        bake(storedToroidalWorld(), Map.of(LevelStem.OVERWORLD, stem(SAME_SCALE, foreignGenerator())));

        DatapackStemOverrides.StemOverride override = DatapackStemOverrides.of(LevelStem.OVERWORLD);
        assertNotNull(override, "the override went unrecorded");
        assertEquals(DatapackStemOverrides.Outcome.REFUSED, override.outcome());
    }

    @Test
    void aBakeWithNoOverrideClearsWhatThePreviousOneRecorded() {
        bake(storedToroidalWorld(), Map.of(LevelStem.OVERWORLD, stem(SAME_SCALE, noiseGenerator(worldgen))));
        assertNotNull(DatapackStemOverrides.of(LevelStem.OVERWORLD),
                "nothing was recorded, so this run says nothing about the clearing");

        bake(storedToroidalWorld(), Map.of(FOREIGN, stem(SAME_SCALE, foreignGenerator())));

        assertNull(DatapackStemOverrides.of(LevelStem.OVERWORLD));
    }

    @Test
    void aStampedOverworldKeepsItsShapeThroughBakeAndReachesItsSiblings() {
        ChunkGenerator overworld = stamped(noiseSubclassGenerator(worldgen), squareTorus(OVERWORLD_CHUNK_WIDTH));
        Registry<LevelStem> baked = bake(
                Map.of(LevelStem.OVERWORLD, stem(SAME_SCALE, overworld)),
                Map.of(FOREIGN, stem(SAME_SCALE, foreignGenerator())));

        FlatShape kept = shapeOf(baked, LevelStem.OVERWORLD);
        assertNotNull(kept, "the stamped overworld lost its shape at bake");
        assertEquals(OVERWORLD_CHUNK_WIDTH, chunkWidth(kept, Direction.Axis.X));

        FlatShape sibling = shapeOf(baked, FOREIGN);
        assertNotNull(sibling, "a stamped overworld derived nothing for its siblings");
        assertEquals(SAME_SCALE_CHUNK_WIDTH, chunkWidth(sibling, Direction.Axis.X));
    }

    @Test
    void aDatapackOverworldTakesTheShapeAStampedStoredOverworldCarries() {
        ChunkGenerator stored = stamped(noiseSubclassGenerator(worldgen),
                squareTorus(STORED_OVERWORLD_CHUNK_WIDTH));
        Registry<LevelStem> baked = bake(
                Map.of(LevelStem.OVERWORLD, stem(SAME_SCALE, stored)),
                Map.of(LevelStem.OVERWORLD, stem(SAME_SCALE, noiseGenerator(worldgen))));

        assertInstanceOf(LoopedChunkGenerator.class, generatorOf(baked, LevelStem.OVERWORLD));

        FlatShape shape = shapeOf(baked, LevelStem.OVERWORLD);
        assertNotNull(shape, "the datapack overworld carries no fold");
        assertEquals(STORED_OVERWORLD_CHUNK_WIDTH, chunkWidth(shape, Direction.Axis.X));
    }

    private static Map<ResourceKey<LevelStem>, LevelStem> storedToroidalWorld() {
        return Map.of(
                LevelStem.OVERWORLD, stem(SAME_SCALE, shapedGenerator(squareTorus(STORED_OVERWORLD_CHUNK_WIDTH))),
                LevelStem.NETHER,
                stem(NETHER_COORDINATE_SCALE, shapedGenerator(squareTorus(STORED_NETHER_CHUNK_WIDTH))),
                LevelStem.END, stem(SAME_SCALE, shapedGenerator(squareTorus(STORED_END_CHUNK_WIDTH))));
    }

    private static FlatShape bakeForeign(double coordinateScale, ChunkGenerator generator) {
        return shapeOf(bake(Map.of(FOREIGN, stem(coordinateScale, generator))), FOREIGN);
    }

    private static Registry<LevelStem> bake(Map<ResourceKey<LevelStem>, LevelStem> datapackStems) {
        return bake(
                Map.of(LevelStem.OVERWORLD, stem(SAME_SCALE, shapedGenerator(squareTorus(OVERWORLD_CHUNK_WIDTH)))),
                datapackStems);
    }

    private static Registry<LevelStem> bake(Map<ResourceKey<LevelStem>, LevelStem> storedStems,
            Map<ResourceKey<LevelStem>, LevelStem> datapackStems) {
        return selected(storedStems).bake(datapackRegistry(datapackStems)).dimensions();
    }
}
