package com.toroidalworld.compat.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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

    private static final WorldLoopBounds BOUNDS =
            new WorldLoopBounds(-WORLD_CHUNKS, WORLD_CHUNKS, -WORLD_CHUNKS, WORLD_CHUNKS);

    private static final WorldFold PER_AXIS = WorldFolds.of(FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW));
    private static final WorldFold DECK_TORUS = new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW));
    private static final WorldFold SKEWED = new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, SKEW_CHUNKS));
    private static final WorldFold MIRRORED =
            new DeckGroupFold(FlatShape.mirrored(BOUNDS, Direction.Axis.Z, MIRROR_LINE_CHUNK));

    private static final List<WorldFold> TRANSLATING = List.of(PER_AXIS, DECK_TORUS, SKEWED);

    private static final List<ResourceKey<Level>> TWO_CARRIAGES = List.of(Level.OVERWORLD, Level.OVERWORLD);

    private static Float[] train(float leadX, float leadZ, float trailX, float trailZ,
            float secondLeadX, float secondLeadZ, float secondTrailX, float secondTrailZ) {
        return new Float[] {
                leadX, HEIGHT, leadZ, trailX, HEIGHT, trailZ,
                secondLeadX, HEIGHT, secondLeadZ, secondTrailX, HEIGHT, secondTrailZ};
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
}
