package com.toroidalworld.compat.create;

import static com.toroidalworld.compat.CompatFoldFixture.DECK_TORUS;
import static com.toroidalworld.compat.CompatFoldFixture.MIRRORED;
import static com.toroidalworld.compat.CompatFoldFixture.PER_AXIS;
import static com.toroidalworld.compat.CompatFoldFixture.SKEWED;
import static com.toroidalworld.compat.CompatFoldFixture.SKEW_CHUNKS;
import static com.toroidalworld.compat.CompatFoldFixture.WORLD_BLOCKS;
import static com.toroidalworld.compat.CompatFoldFixture.WORLD_CHUNKS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

class CreateWalkClosureTest {
    private static CreateWalkClosure closure(WorldFold transformer) {
        return new CreateWalkClosure(transformer);
    }

    private static int walk(CreateWalkClosure closure, BlockPos from, Direction direction, int queries) {
        BlockPos current = from;
        for (int query = 1; query <= queries; query++) {
            current = current.relative(direction);
            if (closure.closes(current)) {
                return query;
            }
        }

        return -1;
    }

    @Test
    void aRingClosesOnTheQueryThatNamesTheStartAgain() {
        for (WorldFold fold : new WorldFold[] {PER_AXIS, DECK_TORUS, SKEWED}) {
            CreateWalkClosure closure = closure(fold);
            BlockPos start = new BlockPos(10, 64, 3);

            int closedAt = walk(closure, start, Direction.WEST, WORLD_BLOCKS + 5);

            assertTrue(closedAt == WORLD_BLOCKS,
                    "closed at query " + closedAt + " of a " + WORLD_BLOCKS + " block ring in " + fold);
        }
    }

    @Test
    void aRowAlongTheGlideAxisOfAMirroredWorldClosesAfterTwoLaps() {
        CreateWalkClosure closure = closure(MIRRORED);
        int ringBlocks = 2 * WORLD_BLOCKS;

        int closedAt = walk(closure, new BlockPos(10, 64, 3), Direction.WEST, ringBlocks + 5);

        assertTrue(closedAt == ringBlocks, "closed at query " + closedAt + " of a " + ringBlocks + " block ring");
    }

    @Test
    void aRowAcrossTheSkewOfALatticeTorusClosesWhenTheShiftsAddUpToWholeLaps() {
        CreateWalkClosure closure = closure(SKEWED);
        int lapsUntilWhole = 2 * WORLD_CHUNKS / gcd(SKEW_CHUNKS, 2 * WORLD_CHUNKS);
        int ringBlocks = lapsUntilWhole * WORLD_BLOCKS;

        int closedAt = walk(closure, new BlockPos(10, 64, 3), Direction.NORTH, ringBlocks + 5);

        assertTrue(closedAt == ringBlocks, "closed at query " + closedAt + " of a " + ringBlocks + " block ring");
    }

    @Test
    void anOpenRowThroughTheSeamNeverCloses() {
        for (WorldFold fold : new WorldFold[] {PER_AXIS, SKEWED, MIRRORED}) {
            CreateWalkClosure closure = closure(fold);

            int closedAt = walk(closure, new BlockPos(250, 64, 3), Direction.EAST, 40);

            assertTrue(closedAt < 0, "closed at query " + closedAt + " in " + fold);
        }
    }

    @Test
    void aVerticalLegNeverCloses() {
        CreateWalkClosure closure = closure(PER_AXIS);

        int closedAt = walk(closure, new BlockPos(0, 0, 0), Direction.UP, WORLD_BLOCKS * 2);

        assertTrue(closedAt < 0, "closed at query " + closedAt);
    }

    @Test
    void theEmitLegOpensAtTheLeftEndAndClosesAfterEveryTube() {
        CreateWalkClosure closure = closure(PER_AXIS);
        BlockPos start = new BlockPos(10, 64, 3);
        walk(closure, start, Direction.WEST, WORLD_BLOCKS);
        BlockPos leftEnd = start.relative(Direction.WEST, WORLD_BLOCKS - 1);

        int closedAt = walk(closure, leftEnd, Direction.EAST, WORLD_BLOCKS + 5);

        assertTrue(closedAt == WORLD_BLOCKS, "closed at query " + closedAt + " of the emit leg");
    }

    @Test
    void aJumpOpensANewLegWithItsOwnOrigin() {
        CreateWalkClosure closure = closure(PER_AXIS);
        walk(closure, new BlockPos(0, 64, 0), Direction.WEST, 100);
        BlockPos secondOrigin = new BlockPos(300, 64, 0);

        assertFalse(closure.closes(secondOrigin.relative(Direction.EAST)));
        int closedAt = walk(closure, secondOrigin.relative(Direction.EAST), Direction.EAST, WORLD_BLOCKS + 5);

        assertTrue(closedAt == WORLD_BLOCKS - 1, "closed at query " + closedAt + " after the jump");
    }

    @Test
    void anUnwrappedLevelNeverCloses() {
        CreateWalkClosure closure = closure(WorldFolds.NOOP);

        int closedAt = walk(closure, new BlockPos(0, 64, 0), Direction.WEST, WORLD_BLOCKS * 4);

        assertTrue(closedAt < 0, "closed at query " + closedAt);
    }

    private static int gcd(int first, int second) {
        return second == 0 ? first : gcd(second, first % second);
    }
}
