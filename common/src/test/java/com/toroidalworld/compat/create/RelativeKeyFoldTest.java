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

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

class RelativeKeyFoldTest {
    private static final int WORLD_CHUNKS = 16;
    private static final int WORLD_BLOCKS = WORLD_CHUNKS * 2 * 16;
    private static final int SKEW_CHUNKS = 4;
    private static final int MIRROR_LINE_CHUNK = 3;

    private static final WorldLoopBounds BOUNDS =
            new WorldLoopBounds(-WORLD_CHUNKS, WORLD_CHUNKS, -WORLD_CHUNKS, WORLD_CHUNKS);

    private static final WorldFold PER_AXIS = WorldFolds.of(FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW));
    private static final WorldFold DECK_TORUS = new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW));
    private static final WorldFold SKEWED = new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, SKEW_CHUNKS));
    private static final WorldFold MIRRORED =
            new DeckGroupFold(FlatShape.mirrored(BOUNDS, Direction.Axis.Z, MIRROR_LINE_CHUNK));

    private static final List<WorldFold> UNSKEWED = List.of(PER_AXIS, DECK_TORUS);
    private static final List<WorldFold> ALL = List.of(PER_AXIS, DECK_TORUS, SKEWED, MIRRORED);

    private static final BlockPos OWNER = new BlockPos(250, 64, 10);
    private static final BlockPos PARTNER_ACROSS = new BlockPos(-254, 64, 10);

    private static BlockPos rawKey(BlockPos owner, BlockPos partner) {
        return owner.subtract(partner);
    }

    @Test
    void aPartnerAcrossTheSeamIsKeyedByTheShortWay() {
        for (WorldFold fold : ALL) {
            BlockPos nearest = fold.nearestCopy(OWNER, PARTNER_ACROSS);
            BlockPos key = RelativeKeyFold.shortWay(fold, OWNER, PARTNER_ACROSS, rawKey(OWNER, PARTNER_ACROSS));

            assertEquals(OWNER.subtract(nearest), key, "in " + fold);
        }
    }

    @Test
    void theShortWayKeyPointsFromThePartnerToTheOwner() {
        for (WorldFold fold : UNSKEWED) {
            BlockPos key = RelativeKeyFold.shortWay(fold, OWNER, PARTNER_ACROSS, rawKey(OWNER, PARTNER_ACROSS));

            assertEquals(new BlockPos(-(WORLD_BLOCKS - 504), 0, 0), key, "in " + fold);
        }
    }

    @Test
    void aPartnerAlreadyNearestGivesTheRawKeyBackByIdentity() {
        BlockPos partner = new BlockPos(240, 64, 10);
        BlockPos raw = rawKey(OWNER, partner);
        for (WorldFold fold : ALL) {
            assertSame(raw, RelativeKeyFold.shortWay(fold, OWNER, partner, raw), "in " + fold);
        }
    }

    @Test
    void anUnwrappedWorldGivesTheRawKeyBackByIdentity() {
        BlockPos raw = rawKey(OWNER, PARTNER_ACROSS);

        assertSame(raw, RelativeKeyFold.shortWay(WorldFolds.NOOP, OWNER, PARTNER_ACROSS, raw));
        assertSame(raw, RelativeKeyFold.shortWay((WorldFold) null, OWNER, PARTNER_ACROSS, raw));
    }

    @Test
    void aLevellessCallGivesTheRawKeyBackByIdentity() {
        BlockPos raw = rawKey(OWNER, PARTNER_ACROSS);

        assertSame(raw, RelativeKeyFold.shortWay((Level) null, OWNER, PARTNER_ACROSS, raw));
        assertSame(raw, RelativeKeyFold.normalize((Level) null, OWNER, raw));
    }

    @Test
    void normalizeRewritesAStoredKeyThatRunsTheLongWayRound() {
        BlockPos stored = rawKey(OWNER, PARTNER_ACROSS);
        for (WorldFold fold : ALL) {
            BlockPos normalized = RelativeKeyFold.normalize(fold, OWNER, stored);

            assertEquals(RelativeKeyFold.shortWay(fold, OWNER, PARTNER_ACROSS, stored), normalized, "in " + fold);
        }
    }

    @Test
    void normalizeIsIdempotentAndGivesAnAlreadyShortKeyBackByIdentity() {
        BlockPos stored = rawKey(OWNER, PARTNER_ACROSS);
        for (WorldFold fold : ALL) {
            BlockPos once = RelativeKeyFold.normalize(fold, OWNER, stored);

            assertSame(once, RelativeKeyFold.normalize(fold, OWNER, once), "in " + fold);
        }
    }

    @Test
    void aNormalizedKeyStillNamesTheSamePhysicalPartner() {
        for (WorldFold fold : ALL) {
            BlockPos stored = rawKey(OWNER, PARTNER_ACROSS);
            BlockPos normalized = RelativeKeyFold.normalize(fold, OWNER, stored);

            assertEquals(fold.fold(PARTNER_ACROSS), fold.fold(OWNER.subtract(normalized)), "in " + fold);
        }
    }
}
