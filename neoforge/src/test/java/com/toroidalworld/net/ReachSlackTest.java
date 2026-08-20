package com.toroidalworld.net;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ReachSlackTest {
    private static final double SECTION_BLOCKS = 16.0;

    private static final double MIRROR_GAP_BLOCKS = SECTION_BLOCKS;

    private static final double ENTITY_DRIFT_BLOCKS = SECTION_BLOCKS;

    private static final double ANCHOR = 0.0;

    private static final double PAST_THE_BOUND_BLOCKS = 1.0;

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
}
