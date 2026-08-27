package com.toroidalworld.compat.create;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.DeckGroupFold;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.shape.FlatShape;
import com.toroidalworld.options.WorldLoopBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

class CreateWalkClosureTest {
    private static final int WORLD_CHUNKS = 16;
    private static final int WORLD_BLOCKS = WORLD_CHUNKS * 2 * 16;
    private static final int SKEW_CHUNKS = 4;
    private static final int MIRROR_LINE_CHUNK = 3;

    private static final WorldLoopBounds BOUNDS =
            new WorldLoopBounds(-WORLD_CHUNKS, WORLD_CHUNKS, -WORLD_CHUNKS, WORLD_CHUNKS);

    private static CreateWalkClosure closure(WorldFold transformer) {
        return new CreateWalkClosure(transformer);
    }

    private static WorldFold loopedWorld() {
        return WorldFolds.of(FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW));
    }

    private static WorldFold deckGroupTorus() {
        return new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW));
    }

    private static WorldFold skewedTorus() {
        return new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, SKEW_CHUNKS));
    }

    private static WorldFold mirroredWorld() {
        return new DeckGroupFold(FlatShape.mirrored(BOUNDS, Direction.Axis.Z, MIRROR_LINE_CHUNK));
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
        for (WorldFold fold : new WorldFold[] {loopedWorld(), deckGroupTorus(), skewedTorus()}) {
            CreateWalkClosure closure = closure(fold);
            BlockPos start = new BlockPos(10, 64, 3);

            int closedAt = walk(closure, start, Direction.WEST, WORLD_BLOCKS + 5);

            assertTrue(closedAt == WORLD_BLOCKS,
                    "closed at query " + closedAt + " of a " + WORLD_BLOCKS + " block ring in " + fold);
        }
    }

    @Test
    void aRowAlongTheGlideAxisOfAMirroredWorldClosesAfterTwoLaps() {
        CreateWalkClosure closure = closure(mirroredWorld());
        int ringBlocks = 2 * WORLD_BLOCKS;

        int closedAt = walk(closure, new BlockPos(10, 64, 3), Direction.WEST, ringBlocks + 5);

        assertTrue(closedAt == ringBlocks, "closed at query " + closedAt + " of a " + ringBlocks + " block ring");
    }

    @Test
    void aRowAcrossTheSkewOfALatticeTorusClosesWhenTheShiftsAddUpToWholeLaps() {
        CreateWalkClosure closure = closure(skewedTorus());
        int lapsUntilWhole = 2 * WORLD_CHUNKS / gcd(SKEW_CHUNKS, 2 * WORLD_CHUNKS);
        int ringBlocks = lapsUntilWhole * WORLD_BLOCKS;

        int closedAt = walk(closure, new BlockPos(10, 64, 3), Direction.NORTH, ringBlocks + 5);

        assertTrue(closedAt == ringBlocks, "closed at query " + closedAt + " of a " + ringBlocks + " block ring");
    }

    @Test
    void anOpenRowThroughTheSeamNeverCloses() {
        for (WorldFold fold : new WorldFold[] {loopedWorld(), skewedTorus(), mirroredWorld()}) {
            CreateWalkClosure closure = closure(fold);

            int closedAt = walk(closure, new BlockPos(250, 64, 3), Direction.EAST, 40);

            assertTrue(closedAt < 0, "closed at query " + closedAt + " in " + fold);
        }
    }

    @Test
    void aVerticalLegNeverCloses() {
        CreateWalkClosure closure = closure(loopedWorld());

        int closedAt = walk(closure, new BlockPos(0, 0, 0), Direction.UP, WORLD_BLOCKS * 2);

        assertTrue(closedAt < 0, "closed at query " + closedAt);
    }

    @Test
    void theEmitLegOpensAtTheLeftEndAndClosesAfterEveryTube() {
        CreateWalkClosure closure = closure(loopedWorld());
        BlockPos start = new BlockPos(10, 64, 3);
        walk(closure, start, Direction.WEST, WORLD_BLOCKS);
        BlockPos leftEnd = start.relative(Direction.WEST, WORLD_BLOCKS - 1);

        int closedAt = walk(closure, leftEnd, Direction.EAST, WORLD_BLOCKS + 5);

        assertTrue(closedAt == WORLD_BLOCKS, "closed at query " + closedAt + " of the emit leg");
    }

    @Test
    void aJumpOpensANewLegWithItsOwnOrigin() {
        CreateWalkClosure closure = closure(loopedWorld());
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
