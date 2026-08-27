package com.toroidalworld.compat.create.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.DeckGroupFold;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

class TrainMapViewFoldTest {
    private static final int WORLD_CHUNKS = 16;
    private static final int WORLD_BLOCKS = WORLD_CHUNKS * 2 * 16;
    private static final int SKEW_CHUNKS = 4;
    private static final int MIRROR_LINE_CHUNK = 3;
    private static final int MIRROR_LINE_BLOCKS = MIRROR_LINE_CHUNK * 16;

    private static final WorldLoopBounds BOUNDS =
            new WorldLoopBounds(-WORLD_CHUNKS, WORLD_CHUNKS, -WORLD_CHUNKS, WORLD_CHUNKS);

    private static final WorldFold PER_AXIS = WorldFolds.of(FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW));
    private static final WorldFold DECK_TORUS = new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW));
    private static final WorldFold SKEWED = new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, SKEW_CHUNKS));
    private static final WorldFold MIRRORED =
            new DeckGroupFold(FlatShape.mirrored(BOUNDS, Direction.Axis.Z, MIRROR_LINE_CHUNK));

    @Test
    void aPixelPastTheXBoundLandsOneWorldWidthBack() {
        for (WorldFold fold : List.of(PER_AXIS, DECK_TORUS, SKEWED)) {
            BlockPos pixel = TrainMapViewFold.wrapPixel(fold, 300, 10);

            assertEquals(new BlockPos(300 - WORLD_BLOCKS, 0, 10), pixel, "in " + fold);
        }
    }

    @Test
    void aPixelPastTheGlideSeamOfAMirroredWorldLandsOnTheMirroredColumn() {
        BlockPos pixel = TrainMapViewFold.wrapPixel(MIRRORED, 300, 10);

        assertEquals(new BlockPos(300 - WORLD_BLOCKS, 0, 2 * MIRROR_LINE_BLOCKS - 10 - 1), pixel);
    }

    @Test
    void aPixelInsideTheBoundsKeepsItsCoordinates() {
        for (WorldFold fold : List.of(PER_AXIS, DECK_TORUS, SKEWED, MIRRORED)) {
            BlockPos pixel = TrainMapViewFold.wrapPixel(fold, 30, 10);

            assertEquals(new BlockPos(30, 0, 10), pixel, "in " + fold);
        }
    }
}
