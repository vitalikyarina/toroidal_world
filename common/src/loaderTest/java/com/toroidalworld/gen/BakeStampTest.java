package com.toroidalworld.gen;

import static com.toroidalworld.gen.BakeStampFixture.OVERWORLD_CHUNK_WIDTH;
import static com.toroidalworld.gen.BakeStampFixture.chunkWidth;
import static com.toroidalworld.gen.BakeStampFixture.datapackRegistry;
import static com.toroidalworld.gen.BakeStampFixture.foreignGenerator;
import static com.toroidalworld.gen.BakeStampFixture.selected;
import static com.toroidalworld.gen.BakeStampFixture.shapeOf;
import static com.toroidalworld.gen.BakeStampFixture.shapedGenerator;
import static com.toroidalworld.gen.BakeStampFixture.squareTorus;
import static com.toroidalworld.gen.BakeStampFixture.stem;
import static com.toroidalworld.gen.BakeStampFixture.stemKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;

@Timeout(60)
class BakeStampTest {
    private static final double SAME_SCALE = 1.0;
    private static final double HALF_SCALE = 0.5;
    private static final double DOUBLE_SCALE = 2.0;
    private static final double TRIPLE_SCALE = 3.0;

    private static final int SAME_SCALE_CHUNK_WIDTH = 32;
    private static final int DOUBLE_SCALE_CHUNK_WIDTH = 16;
    private static final int HALF_SCALE_CHUNK_WIDTH = 64;

    private static final int CODEC_CARRIED_CHUNK_WIDTH = 64;

    private static final ResourceKey<LevelStem> FOREIGN = stemKey("foreign");
    private static final ResourceKey<LevelStem> SIBLING = stemKey("sibling");

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

        Registry<LevelStem> baked = new WorldDimensionsUnderTest(
                stem(SAME_SCALE, foreignGenerator()),
                Map.of(FOREIGN, stem(SAME_SCALE, foreignGenerator()))).bake();

        assertNull(shapeOf(baked, FOREIGN));
    }

    private static FlatShape bakeForeign(double coordinateScale, ChunkGenerator generator) {
        return shapeOf(bake(Map.of(FOREIGN, stem(coordinateScale, generator))), FOREIGN);
    }

    private static Registry<LevelStem> bake(Map<ResourceKey<LevelStem>, LevelStem> datapackStems) {
        return new WorldDimensionsUnderTest(
                stem(SAME_SCALE, shapedGenerator(squareTorus(OVERWORLD_CHUNK_WIDTH))), datapackStems).bake();
    }

    private record WorldDimensionsUnderTest(LevelStem overworld,
            Map<ResourceKey<LevelStem>, LevelStem> datapackStems) {
        private Registry<LevelStem> bake() {
            return selected(Map.of(LevelStem.OVERWORLD, this.overworld))
                    .bake(datapackRegistry(this.datapackStems))
                    .dimensions();
        }
    }
}
