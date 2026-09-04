package com.toroidalworld.net;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WrapDomain;

class ReachSlackTest {
    private static final double SECTION_BLOCKS = 16.0;

    private static final double MIRROR_GAP_BLOCKS = SECTION_BLOCKS;

    private static final double ENTITY_DRIFT_BLOCKS = SECTION_BLOCKS;

    private static final double ANCHOR = 0.0;

    private static final double PAST_THE_BOUND_BLOCKS = 1.0;

    private static final WrapDomain BLOCKS_OF_A_512_WIDE_WORLD = new WrapDomain(-256, 256);

    private static final WrapDomain BLOCKS_OF_A_576_WIDE_WORLD = new WrapDomain(-288, 288);

    private static final WrapDomain BLOCKS_OF_A_4096_WIDE_WORLD = new WrapDomain(-2048, 2048);

    private static final WrapDomain CHUNKS_OF_A_512_WIDE_WORLD = new WrapDomain(-16, 16);

    private static final WrapDomain UNBOUNDED_AXIS = new WrapDomain.Noop();

    private static void assertBound(PacketReach reach, double boundBlocks) {
        assertTrue(TranslationContext.withinReach(boundBlocks, ANCHOR, reach));
        assertTrue(TranslationContext.withinReach(-boundBlocks, ANCHOR, reach));
        assertFalse(TranslationContext.withinReach(boundBlocks + PAST_THE_BOUND_BLOCKS, ANCHOR, reach));
        assertFalse(TranslationContext.withinReach(-boundBlocks - PAST_THE_BOUND_BLOCKS, ANCHOR, reach));
    }

    @Nested
    class TrackedEntities {
        @Test
        void reachesTwoSectionsPastTheView() {
            assertBound(PacketReach.tracked(12), 192.0 + MIRROR_GAP_BLOCKS + ENTITY_DRIFT_BLOCKS);
        }

        @Test
        void carriesOneSectionMoreThanTheFamiliesGatedAtTheSend() {
            assertBound(PacketReach.tracked(2), 32.0 + MIRROR_GAP_BLOCKS + ENTITY_DRIFT_BLOCKS);
            assertFalse(TranslationContext.withinReach(
                    32.0 + MIRROR_GAP_BLOCKS + ENTITY_DRIFT_BLOCKS,
                    ANCHOR,
                    PacketReach.PARTICLE));
        }
    }

    @Nested
    class GatedAtTheSend {
        @Test
        void particlesReachOneSectionPastTheirRadius() {
            assertBound(PacketReach.PARTICLE, 32.0 + MIRROR_GAP_BLOCKS);
            assertBound(PacketReach.FORCED_PARTICLE, 512.0 + MIRROR_GAP_BLOCKS);
        }

        @Test
        void explosionsReachOneSectionPastTheirRadius() {
            assertBound(PacketReach.EXPLOSION, 64.0 + MIRROR_GAP_BLOCKS);
        }

        @Test
        void soundsReachOneSectionPastTheRangeTheyCarry() {
            assertBound(PacketReach.sound(16.0F), 16.0 + MIRROR_GAP_BLOCKS);
            assertBound(PacketReach.sound(256.0F), 256.0 + MIRROR_GAP_BLOCKS);
        }
    }

    @Nested
    class TheAxisCarriesTheGuard {
        @Test
        void theWorldFromTheLogCarriesNeitherViewDistanceItWasSeenWith() {
            assertFalse(TranslationContext.carriesReach(BLOCKS_OF_A_512_WIDE_WORLD, PacketReach.tracked(13)));
            assertFalse(TranslationContext.carriesReach(BLOCKS_OF_A_512_WIDE_WORLD, PacketReach.tracked(12)));
        }

        @Test
        void aCoordinateOutOfReachInThatWorldIsStillOutOfReach() {
            assertFalse(TranslationContext.withinReach(-312.5, -65.15, PacketReach.tracked(13)));
        }

        @Test
        void twiceTheBudgetPlusTheMarginIsWhereTheAxisStartsCarrying() {
            assertTrue(TranslationContext.carriesReach(BLOCKS_OF_A_576_WIDE_WORLD, PacketReach.tracked(13)));
            assertTrue(TranslationContext.carriesReach(BLOCKS_OF_A_4096_WIDE_WORLD, PacketReach.tracked(13)));
        }

        @Test
        void anUnboundedAxisCarriesEveryReach() {
            assertTrue(TranslationContext.carriesReach(UNBOUNDED_AXIS, PacketReach.tracked(13)));
            assertTrue(TranslationContext.carriesReach(UNBOUNDED_AXIS, PacketReach.FORCED_PARTICLE));
        }

        @Test
        void theChunkViewGoesSilentInsideTheBandTheWorldReserved() {
            assertFalse(TranslationContext.carriesView(CHUNKS_OF_A_512_WIDE_WORLD, 14));
            assertTrue(TranslationContext.carriesView(CHUNKS_OF_A_512_WIDE_WORLD, 10));
            assertTrue(TranslationContext.carriesView(UNBOUNDED_AXIS, 14));
        }
    }
}
