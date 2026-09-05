package com.toroidalworld.compat.create;

import static com.toroidalworld.compat.CompatFoldFixture.DECK_TORUS;
import static com.toroidalworld.compat.CompatFoldFixture.MIRRORED;
import static com.toroidalworld.compat.CompatFoldFixture.PER_AXIS;
import static com.toroidalworld.compat.CompatFoldFixture.SKEWED;
import static com.toroidalworld.compat.CompatFoldFixture.WORLD_BLOCKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

class RelativeKeyFoldTest {
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
