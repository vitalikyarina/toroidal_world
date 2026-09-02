package com.toroidalworld.compat.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.DeckGroupFold;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

class TrainMapSyncFoldTest {
    private static final int WORLD_CHUNKS = 16;
    private static final int WORLD_BLOCKS = WORLD_CHUNKS * 2 * 16;
    private static final int SKEW_CHUNKS = 4;
    private static final int MIRROR_LINE_CHUNK = 3;
    private static final int MIRROR_LINE_BLOCKS = MIRROR_LINE_CHUNK * 16;
    private static final float HEIGHT = 64.0F;
    private static final float TRACK_Z = 10.0F;
    private static final int FLOATS_PER_BOGEY = 3;
    private static final int FLOATS_PER_CARRIAGE = 6;

    private static final WorldLoopBounds BOUNDS =
            new WorldLoopBounds(-WORLD_CHUNKS, WORLD_CHUNKS, -WORLD_CHUNKS, WORLD_CHUNKS);

    private static final WorldFold PER_AXIS = WorldFolds.of(FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW));
    private static final WorldFold DECK_TORUS = new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW));
    private static final WorldFold SKEWED = new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, SKEW_CHUNKS));
    private static final WorldFold MIRRORED =
            new DeckGroupFold(FlatShape.mirrored(BOUNDS, Direction.Axis.Z, MIRROR_LINE_CHUNK));

    private static final List<WorldFold> TRANSLATING = List.of(PER_AXIS, DECK_TORUS, SKEWED);

    private static final List<ResourceKey<Level>> TWO_CARRIAGES = List.of(Level.OVERWORLD, Level.OVERWORLD);

    private static final List<ResourceKey<Level>> FOUR_CARRIAGES =
            List.of(Level.OVERWORLD, Level.OVERWORLD, Level.OVERWORLD, Level.OVERWORLD);

    private static final List<ResourceKey<Level>> ACROSS_A_PORTAL =
            List.of(Level.OVERWORLD, Level.OVERWORLD, Level.NETHER);

    private static final List<ResourceKey<Level>> A_CARRIAGE_IN_NO_DIMENSION =
            Arrays.asList(Level.OVERWORLD, null, Level.OVERWORLD);

    private static Float[] train(float leadX, float leadZ, float trailX, float trailZ,
            float secondLeadX, float secondLeadZ, float secondTrailX, float secondTrailZ) {
        return new Float[] {
                leadX, HEIGHT, leadZ, trailX, HEIGHT, trailZ,
                secondLeadX, HEIGHT, secondLeadZ, secondTrailX, HEIGHT, secondTrailZ};
    }

    private static Float[] bogeysAt(float... trackXs) {
        Float[] positions = new Float[trackXs.length * FLOATS_PER_BOGEY];
        for (int bogey = 0; bogey < trackXs.length; bogey++) {
            positions[bogey * FLOATS_PER_BOGEY] = trackXs[bogey];
            positions[bogey * FLOATS_PER_BOGEY + 1] = HEIGHT;
            positions[bogey * FLOATS_PER_BOGEY + 2] = TRACK_Z;
        }

        return positions;
    }

    private static void assertTrackXs(Float[] positions, WorldFold fold, float... trackXs) {
        for (int bogey = 0; bogey < trackXs.length; bogey++) {
            assertEquals(trackXs[bogey], positions[bogey * FLOATS_PER_BOGEY],
                    "bogey " + bogey + " x in " + fold);
            assertEquals(TRACK_Z, positions[bogey * FLOATS_PER_BOGEY + 2],
                    "bogey " + bogey + " z in " + fold);
        }
    }

    @Test
    void aCarriageAcrossTheSeamIsWrittenBesideTheLeadingOne() {
        for (WorldFold fold : TRANSLATING) {
            Float[] positions = train(250, 10, 254, 10, -254, 10, -250, 10);

            TrainMapSyncFold.coherent(positions, TWO_CARRIAGES, dimension -> fold);

            assertEquals(-254 + WORLD_BLOCKS, positions[6], "second leading x in " + fold);
            assertEquals(10, positions[8], "second leading z in " + fold);
            assertEquals(-250 + WORLD_BLOCKS, positions[9], "second trailing x in " + fold);
            assertEquals(10, positions[11], "second trailing z in " + fold);
        }
    }

    @Test
    void aCarriageAcrossTheGlideSeamOfAMirroredWorldIsWrittenMirrored() {
        Float[] positions = train(250, 10, 254, 10, -254, 10, -250, 10);
        float mirroredZ = 2 * MIRROR_LINE_BLOCKS - 10;

        TrainMapSyncFold.coherent(positions, TWO_CARRIAGES, dimension -> MIRRORED);

        assertEquals(-254 + WORLD_BLOCKS, positions[6], "second leading x");
        assertEquals(mirroredZ, positions[8], "second leading z");
        assertEquals(-250 + WORLD_BLOCKS, positions[9], "second trailing x");
        assertEquals(mirroredZ, positions[11], "second trailing z");
    }

    @Test
    void aTrainInsideTheBoundsIsUntouched() {
        for (WorldFold fold : List.of(PER_AXIS, DECK_TORUS, SKEWED, MIRRORED)) {
            Float[] positions = train(10, 10, 14, 10, 18, 10, 22, 10);
            Float[] before = positions.clone();

            TrainMapSyncFold.coherent(positions, TWO_CARRIAGES, dimension -> fold);

            for (int index = 0; index < positions.length; index++) {
                assertSame(before[index], positions[index], "float " + index + " in " + fold);
            }
        }
    }

    @Test
    void aStalePositionIsRebasedOntoTheCurrentCopy() {
        for (WorldFold fold : TRANSLATING) {
            Float[] stale = train(250, 10, 254, 10, -254, 10, -250, 10);
            Float[] current = train(252, 10, 256, 10, 260, 10, 264, 10);

            TrainMapSyncFold.rebaseOnto(stale, current, TWO_CARRIAGES, dimension -> fold);

            assertEquals(-254 + WORLD_BLOCKS, stale[6], "second leading x in " + fold);
            assertEquals(-250 + WORLD_BLOCKS, stale[9], "second trailing x in " + fold);
        }
    }

    @Test
    void aTrainLongerThanHalfTheWorldChainsCarriageToCarriage() {
        for (WorldFold fold : TRANSLATING) {
            Float[] positions = bogeysAt(200, 240, -192, -152, -112, -72, -32, 8);

            TrainMapSyncFold.coherent(positions, FOUR_CARRIAGES, dimension -> fold);

            assertTrackXs(positions, fold, 200, 240, 320, 360, 400, 440, 480, 520);
        }
    }

    @Test
    void aCarriageInAnotherDimensionStartsItsOwnChain() {
        for (WorldFold fold : TRANSLATING) {
            Float[] positions = bogeysAt(200, 240, -192, -152, -112, -72);

            TrainMapSyncFold.coherent(positions, ACROSS_A_PORTAL, dimension -> fold);

            assertTrackXs(positions, fold, 200, 240, 320, 360, -112, -72);
        }
    }

    @Test
    void aCarriageWithNoDimensionNeverBecomesTheAnchor() {
        for (WorldFold fold : TRANSLATING) {
            Float[] positions = bogeysAt(200, 240, 0, 0, -192, -152);
            Arrays.fill(positions, FLOATS_PER_CARRIAGE, 2 * FLOATS_PER_CARRIAGE, 0.0F);

            TrainMapSyncFold.coherent(positions, A_CARRIAGE_IN_NO_DIMENSION, dimension -> fold);

            assertEquals(0.0F, positions[6], "skipped leading x in " + fold);
            assertEquals(0.0F, positions[9], "skipped trailing x in " + fold);
            assertEquals(-192 + WORLD_BLOCKS, positions[12], "third leading x in " + fold);
            assertEquals(-152 + WORLD_BLOCKS, positions[15], "third trailing x in " + fold);
        }
    }
}
