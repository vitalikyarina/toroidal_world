package com.toroidalworld.compat.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.DeckGroupFold;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

class CrafterGroupFoldTest {
    private static final int WORLD_CHUNKS = 16;
    private static final int WORLD_BLOCKS = WORLD_CHUNKS * 2 * 16;
    private static final int SKEW_CHUNKS = 4;
    private static final int MIRROR_LINE_CHUNK = 3;

    private static final WorldLoopBounds BOUNDS =
            new WorldLoopBounds(-WORLD_CHUNKS, WORLD_CHUNKS, -WORLD_CHUNKS, WORLD_CHUNKS);
    private static final WorldLoopBounds X_ONLY =
            new WorldLoopBounds(new AxisBounds.Looped(-WORLD_CHUNKS, WORLD_CHUNKS), AxisBounds.Unbounded.INSTANCE);

    private static final WorldFold PER_AXIS = WorldFolds.of(FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW));
    private static final WorldFold DECK_TORUS = new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW));
    private static final WorldFold SKEWED = new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, SKEW_CHUNKS));
    private static final WorldFold MIRRORED =
            new DeckGroupFold(FlatShape.mirrored(BOUNDS, Direction.Axis.Z, MIRROR_LINE_CHUNK));
    private static final WorldFold CYLINDER = WorldFolds.of(FlatShape.cylinder(X_ONLY));

    private static final List<WorldFold> UNSKEWED = List.of(PER_AXIS, DECK_TORUS);
    private static final List<WorldFold> ALL = List.of(PER_AXIS, DECK_TORUS, SKEWED, MIRRORED, CYLINDER);

    private static final BlockPos OWNER = new BlockPos(250, 64, 10);
    private static final BlockPos MEMBER_ACROSS = new BlockPos(-254, 64, 10);
    private static final BlockPos MEMBER_INSIDE = new BlockPos(240, 64, 10);

    private static BlockPos storedDelta(BlockPos owner, BlockPos member) {
        return member.subtract(owner);
    }

    @Test
    void theOwnerAndTheMemberBothSitInsideTheBounds() {
        for (WorldFold fold : ALL) {
            assertFalse(fold.isOver(OWNER), "owner in " + fold);
            assertFalse(fold.isOver(MEMBER_ACROSS), "member in " + fold);
        }
    }

    @Test
    void aStoredDeltaTheLongWayRoundIsRewrittenToTheShortWay() {
        BlockPos raw = storedDelta(OWNER, MEMBER_ACROSS);
        for (WorldFold fold : UNSKEWED) {
            assertEquals(new BlockPos(WORLD_BLOCKS - 504, 0, 0),
                    CrafterGroupFold.foldStoredDelta(fold, OWNER, raw), "in " + fold);
        }
    }

    @Test
    void theRewrittenDeltaLandsOnTheCopyOfTheMemberTheFoldNames() {
        BlockPos raw = storedDelta(OWNER, MEMBER_ACROSS);
        for (WorldFold fold : ALL) {
            BlockPos folded = CrafterGroupFold.foldStoredDelta(fold, OWNER, raw);

            assertEquals(fold.nearestCopy(OWNER, MEMBER_ACROSS), OWNER.offset(folded), "in " + fold);
        }
    }

    @Test
    void aFoldedDeltaRefoldsToItselfByIdentity() {
        BlockPos raw = storedDelta(OWNER, MEMBER_ACROSS);
        for (WorldFold fold : ALL) {
            BlockPos folded = CrafterGroupFold.foldStoredDelta(fold, OWNER, raw);

            assertSame(folded, CrafterGroupFold.foldStoredDelta(fold, OWNER, folded), "in " + fold);
        }
    }

    @Test
    void aMemberAlreadyNearestGivesTheStoredDeltaBackByIdentity() {
        BlockPos raw = storedDelta(OWNER, MEMBER_INSIDE);
        for (WorldFold fold : ALL) {
            assertSame(raw, CrafterGroupFold.foldStoredDelta(fold, OWNER, raw), "in " + fold);
        }
    }

    @Test
    void anUnwrappedWorldGivesTheStoredDeltaBackByIdentity() {
        BlockPos raw = storedDelta(OWNER, MEMBER_ACROSS);

        assertSame(raw, CrafterGroupFold.foldStoredDelta(WorldFolds.NOOP, OWNER, raw));
        assertSame(raw, CrafterGroupFold.foldStoredDelta((WorldFold) null, OWNER, raw));
    }
}
