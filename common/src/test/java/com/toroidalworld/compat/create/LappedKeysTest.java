package com.toroidalworld.compat.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.DeckGroupFold;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.BlockPos;

class LappedKeysTest {
    private static final int WORLD_CHUNKS = 16;
    private static final int WORLD_BLOCKS = WORLD_CHUNKS * 2 * 16;

    private static final WorldLoopBounds BOUNDS =
            new WorldLoopBounds(-WORLD_CHUNKS, WORLD_CHUNKS, -WORLD_CHUNKS, WORLD_CHUNKS);
    private static final FlatShape TORUS = FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW);

    private static final List<WorldFold> WRAPPING = List.of(WorldFolds.of(TORUS), new DeckGroupFold(TORUS));

    private static final BlockPos INSIDE = new BlockPos(250, 64, 10);
    private static final BlockPos ONE_LAP_ON = new BlockPos(250 + WORLD_BLOCKS, 64, 10);
    private static final BlockPos ANOTHER_BLOCK = new BlockPos(249, 64, 10);

    @Test
    void aNameOneLapOnIsRefusedTheClaimAndWitnessedAsALap() {
        for (WorldFold fold : WRAPPING) {
            LappedKeys keys = new LappedKeys(fold);

            assertTrue(keys.add(INSIDE), "in " + fold);
            assertFalse(keys.add(ONE_LAP_ON), "in " + fold);
            assertEquals(1, keys.size(), "in " + fold);
            assertEquals(new LappedKeys.Lap(ONE_LAP_ON, INSIDE, INSIDE), keys.lapped(), "in " + fold);
        }
    }

    @Test
    void containsAnswersForANameOneLapOnAndWitnessesIt() {
        for (WorldFold fold : WRAPPING) {
            LappedKeys keys = new LappedKeys(fold);
            keys.add(INSIDE);

            assertTrue(keys.contains(ONE_LAP_ON), "in " + fold);
            assertEquals(new LappedKeys.Lap(ONE_LAP_ON, INSIDE, INSIDE), keys.lapped(), "in " + fold);
        }
    }

    @Test
    void theSameNameTwiceIsNotALap() {
        for (WorldFold fold : WRAPPING) {
            LappedKeys keys = new LappedKeys(fold);
            keys.add(INSIDE);

            assertFalse(keys.add(INSIDE), "in " + fold);
            assertTrue(keys.contains(INSIDE), "in " + fold);
            assertNull(keys.lapped(), "in " + fold);
        }
    }

    @Test
    void twoDifferentPhysicalBlocksNeverWitness() {
        for (WorldFold fold : WRAPPING) {
            LappedKeys keys = new LappedKeys(fold);

            assertTrue(keys.add(INSIDE), "in " + fold);
            assertTrue(keys.add(ANOTHER_BLOCK), "in " + fold);
            assertEquals(2, keys.size(), "in " + fold);
            assertNull(keys.lapped(), "in " + fold);
        }
    }

    @Test
    void theSetHandsBackTheRawNamesThatClaimedItsKeys() {
        for (WorldFold fold : WRAPPING) {
            LappedKeys keys = new LappedKeys(fold);
            keys.add(ONE_LAP_ON);
            keys.add(ANOTHER_BLOCK);

            assertEquals(Set.of(ONE_LAP_ON, ANOTHER_BLOCK), Set.copyOf(keys), "in " + fold);
        }
    }

    @Test
    void aFoldThatWrapsNothingKeepsBothNamesAndWitnessesNothing() {
        LappedKeys keys = new LappedKeys(WorldFolds.NOOP);

        assertTrue(keys.add(INSIDE));
        assertTrue(keys.add(ONE_LAP_ON));
        assertFalse(keys.contains(new BlockPos(250 + 2 * WORLD_BLOCKS, 64, 10)));
        assertEquals(2, keys.size());
        assertNull(keys.lapped());
    }
}
