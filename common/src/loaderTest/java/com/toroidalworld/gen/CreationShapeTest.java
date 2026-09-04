package com.toroidalworld.gen;

import static com.toroidalworld.gen.BakeStampFixture.foreignGenerator;
import static com.toroidalworld.gen.BakeStampFixture.noiseGenerator;
import static com.toroidalworld.gen.BakeStampFixture.noiseSubclassGenerator;
import static com.toroidalworld.gen.BakeStampFixture.selected;
import static com.toroidalworld.gen.BakeStampFixture.squareTorus;
import static com.toroidalworld.gen.BakeStampFixture.stem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.toroidalworld.shape.FlatShape;
import com.toroidalworld.shape.torus.TorusDimensions;
import com.toroidalworld.shape.torus.TorusSettings;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

@Timeout(60)
class CreationShapeTest {
    private static final double OVERWORLD_SCALE = 1.0;
    private static final double NETHER_COORDINATE_SCALE = 8.0;

    private static final int CHOSEN_CHUNK_WIDTH = 64;

    private static HolderLookup.Provider worldgen;

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        worldgen = VanillaRegistries.createLookup();
    }

    @Test
    void aNoiseSubclassCanTakeAShapeAndAForeignGeneratorCannot() {
        assertTrue(ShapedDimensions.canTakeShape(noiseSubclassGenerator(worldgen)));
        assertTrue(ShapedDimensions.canTakeShape(noiseGenerator(worldgen)));
        assertFalse(ShapedDimensions.canTakeShape(foreignGenerator()));
    }

    @Test
    void aNoiseSubclassOverworldKeepsItsClassAndTakesTheShape() {
        ChunkGenerator overworld = noiseSubclassGenerator(worldgen);
        WorldDimensions dimensions = overworldOnly(overworld);
        FlatShape shape = squareTorus(CHOSEN_CHUNK_WIDTH);

        WorldDimensions shaped = ShapedDimensions.withShape(dimensions, LevelStem.OVERWORLD, shape);

        assertNotSame(dimensions, shaped, "the shaped dimensions are the object the apply guard compares");
        assertEquals(shape, ShapedDimensions.shapeOf(shaped, LevelStem.OVERWORLD));
        assertSame(overworld, shaped.get(LevelStem.OVERWORLD).orElseThrow().generator());
    }

    @Test
    void aForeignOverworldIsLeftUnshaped() {
        WorldDimensions dimensions = overworldOnly(foreignGenerator());

        assertSame(dimensions,
                ShapedDimensions.withShape(dimensions, LevelStem.OVERWORLD, squareTorus(CHOSEN_CHUNK_WIDTH)));
    }

    @Test
    void aTorusReachesEveryStemOverANoiseSubclassOverworld() {
        WorldDimensions shaped = TorusDimensions.apply(vanillaThree(noiseSubclassGenerator(worldgen)),
                TorusSettings.DEFAULT);

        assertNotNull(ShapedDimensions.shapeOf(shaped, LevelStem.OVERWORLD), "the overworld carries no shape");
        assertNotNull(ShapedDimensions.shapeOf(shaped, LevelStem.NETHER), "the nether was never reached");
        assertNotNull(ShapedDimensions.shapeOf(shaped, LevelStem.END), "the End was never reached");
    }

    @Test
    void strippingClearsAShapeStampedByAnEarlierAttempt() {
        ChunkGenerator overworld = noiseSubclassGenerator(worldgen);
        WorldDimensions shaped = ShapedDimensions.withShape(overworldOnly(overworld), LevelStem.OVERWORLD,
                squareTorus(CHOSEN_CHUNK_WIDTH));

        assertNull(ShapedDimensions.shapeOf(ShapedDimensions.stripShapes(shaped), LevelStem.OVERWORLD));
    }

    private static WorldDimensions overworldOnly(ChunkGenerator overworld) {
        return selected(Map.of(LevelStem.OVERWORLD, stem(OVERWORLD_SCALE, overworld)));
    }

    private static WorldDimensions vanillaThree(ChunkGenerator overworld) {
        return selected(Map.of(
                LevelStem.OVERWORLD, stem(OVERWORLD_SCALE, overworld),
                LevelStem.NETHER, stem(NETHER_COORDINATE_SCALE, noiseGenerator(worldgen)),
                LevelStem.END, stem(OVERWORLD_SCALE, noiseGenerator(worldgen))));
    }
}
