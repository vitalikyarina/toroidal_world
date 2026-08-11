package com.toroidalworld.net;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// The slack each packet family is allowed past its own radius, stated as the terms it was derived from rather than as
// the numbers that hold it. A family whose slack is flattened back into one shared constant fails here.
class ReachSlackTest {
    private static final double SECTION_BLOCKS = 16.0;

    // ChunkMap.tick refreshes a player's tracking state when their section changes, so the server position the send
    // site measured from and the mirror this guard measures from can stand a section apart. Every family owes this.
    private static final double MIRROR_GAP_BLOCKS = SECTION_BLOCKS;

    // The tracked family alone owes a second section: its radius is a standing decision, refreshed when the entity
    // changes section, so between two refreshes the entity travels that much further out than the decision measured.
    private static final double ENTITY_DRIFT_BLOCKS = SECTION_BLOCKS;

    private static final double ANCHOR = 0.0;

    private static final double PAST_THE_BOUND_BLOCKS = 1.0;

    private static void assertBound(PacketReach reach, double slackBlocks) {
        double bound = reach.blocks() + slackBlocks;
        assertTrue(TranslationContext.withinReach(bound, ANCHOR, reach));
        assertTrue(TranslationContext.withinReach(-bound, ANCHOR, reach));
        assertFalse(TranslationContext.withinReach(bound + PAST_THE_BOUND_BLOCKS, ANCHOR, reach));
        assertFalse(TranslationContext.withinReach(-bound - PAST_THE_BOUND_BLOCKS, ANCHOR, reach));
    }

    @Nested
    class TrackedEntities {
        @Test
        void reachesTwoSectionsPastTheView() {
            // View distance 12 chunks (192 blocks), so the bound is 192 + 16 + 16 = 224 blocks.
            assertBound(PacketReach.tracked(12), MIRROR_GAP_BLOCKS + ENTITY_DRIFT_BLOCKS);
        }

        @Test
        void carriesOneSectionMoreThanTheFamiliesGatedAtTheSend() {
            assertBound(PacketReach.tracked(2), MIRROR_GAP_BLOCKS + ENTITY_DRIFT_BLOCKS);
            assertFalse(TranslationContext.withinReach(
                    PacketReach.PARTICLE.blocks() + MIRROR_GAP_BLOCKS + ENTITY_DRIFT_BLOCKS,
                    ANCHOR,
                    PacketReach.PARTICLE));
        }
    }

    @Nested
    class GatedAtTheSend {
        @Test
        void particlesReachOneSectionPastTheirRadius() {
            assertBound(PacketReach.PARTICLE, MIRROR_GAP_BLOCKS);
            assertBound(PacketReach.FORCED_PARTICLE, MIRROR_GAP_BLOCKS);
        }

        @Test
        void explosionsReachOneSectionPastTheirRadius() {
            assertBound(PacketReach.EXPLOSION, MIRROR_GAP_BLOCKS);
        }

        @Test
        void soundsReachOneSectionPastTheRangeTheyCarry() {
            assertBound(PacketReach.sound(16.0F), MIRROR_GAP_BLOCKS);
            assertBound(PacketReach.sound(256.0F), MIRROR_GAP_BLOCKS);
        }
    }
}
