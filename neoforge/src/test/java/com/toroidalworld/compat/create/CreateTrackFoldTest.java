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
import net.minecraft.core.Vec3i;

class CreateTrackFoldTest {
    private static final int WORLD_CHUNKS = 16;
    private static final int WORLD_BLOCKS = WORLD_CHUNKS * 2 * 16;
    private static final int SKEW_CHUNKS = 4;
    private static final int MIRROR_LINE_CHUNK = 3;
    private static final int MIRROR_LINE_BLOCKS = MIRROR_LINE_CHUNK * 16;
    private static final int KEY_Y = 128;

    private static final WorldLoopBounds BOUNDS =
            new WorldLoopBounds(-WORLD_CHUNKS, WORLD_CHUNKS, -WORLD_CHUNKS, WORLD_CHUNKS);

    private static final WorldFold PER_AXIS = WorldFolds.of(FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW));
    private static final WorldFold DECK_TORUS = new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW));
    private static final WorldFold SKEWED = new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, SKEW_CHUNKS));
    private static final WorldFold MIRRORED =
            new DeckGroupFold(FlatShape.mirrored(BOUNDS, Direction.Axis.Z, MIRROR_LINE_CHUNK));

    private static final List<WorldFold> TRANSLATING = List.of(PER_AXIS, DECK_TORUS, SKEWED);

    private static Vec3i keyAt(double x, double z) {
        return new Vec3i((int) Math.round(x * 2), KEY_Y, (int) Math.round(z * 2));
    }

    @Test
    void aKeyPastTheXBoundIsWrappedByOneWorldWidth() {
        for (WorldFold fold : TRANSLATING) {
            Vec3i canonical = CreateTrackFold.canonicalNodeKey(fold, keyAt(300.5, 10.5));

            assertEquals(keyAt(300.5 - WORLD_BLOCKS, 10.5), canonical, "in " + fold);
        }
    }

    @Test
    void aKeyPastTheGlideSeamOfAMirroredWorldComesBackMirrored() {
        Vec3i canonical = CreateTrackFold.canonicalNodeKey(MIRRORED, keyAt(300.5, 10.5));

        assertEquals(keyAt(300.5 - WORLD_BLOCKS, 2 * MIRROR_LINE_BLOCKS - 10.5), canonical);
    }

    @Test
    void aKeyInsideTheBoundsIsReturnedItself() {
        Vec3i key = keyAt(10.5, 10.5);
        for (WorldFold fold : List.of(PER_AXIS, DECK_TORUS, SKEWED, MIRRORED)) {
            assertSame(key, CreateTrackFold.canonicalNodeKey(fold, key), "in " + fold);
        }
    }

    @Test
    void theNearestKeyToAnAnchorAcrossTheSeamIsTheCopyBesideIt() {
        Vec3i anchor = keyAt(250.5, 10.5);
        for (WorldFold fold : TRANSLATING) {
            Vec3i nearest = CreateTrackFold.nearestNodeKey(fold, anchor, keyAt(-254.5, 10.5));

            assertEquals(keyAt(-254.5 + WORLD_BLOCKS, 10.5), nearest, "in " + fold);
        }
    }

    @Test
    void theNearestKeyAcrossTheGlideSeamIsMirrored() {
        Vec3i nearest = CreateTrackFold.nearestNodeKey(MIRRORED, keyAt(250.5, 10.5), keyAt(-254.5, 10.5));

        assertEquals(keyAt(-254.5 + WORLD_BLOCKS, 2 * MIRROR_LINE_BLOCKS - 10.5), nearest);
    }

    @Test
    void aKeyAlreadyBesideItsAnchorIsReturnedItself() {
        Vec3i key = keyAt(14.5, 10.5);
        for (WorldFold fold : List.of(PER_AXIS, DECK_TORUS, SKEWED, MIRRORED)) {
            assertSame(key, CreateTrackFold.nearestNodeKey(fold, keyAt(10.5, 10.5), key), "in " + fold);
        }
    }
}
