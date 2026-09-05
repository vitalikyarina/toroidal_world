package com.toroidalworld.compat.create;

import static com.toroidalworld.compat.CompatFoldFixture.CYLINDER;
import static com.toroidalworld.compat.CompatFoldFixture.DECK_TORUS;
import static com.toroidalworld.compat.CompatFoldFixture.MIRRORED;
import static com.toroidalworld.compat.CompatFoldFixture.PER_AXIS;
import static com.toroidalworld.compat.CompatFoldFixture.SKEWED;
import static com.toroidalworld.compat.CompatFoldFixture.WORLD_BLOCKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;

import net.minecraft.core.BlockPos;

class CrafterGroupFoldTest {
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
