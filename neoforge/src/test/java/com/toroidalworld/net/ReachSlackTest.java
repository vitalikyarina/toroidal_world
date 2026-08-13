package com.toroidalworld.net;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// The slack each packet family is allowed past its own radius. Every bound below is stated by hand — the radius as
// the literal the vanilla send site fixes, the slack as the terms it was derived from — because a bound built on
// reach.blocks() stands on both sides of the comparison and cancels: a drifted radius then holds its own drifted
// bound and passes unseen. A family whose slack is flattened back into one shared constant fails here too.
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
            // View distance 12 chunks is 192 blocks, multiplied out by hand so the radius cannot cancel out of its
            // own bound: 192 + 16 + 16 = 224 blocks.
            assertBound(PacketReach.tracked(12), 192.0 + MIRROR_GAP_BLOCKS + ENTITY_DRIFT_BLOCKS);
        }

        @Test
        void carriesOneSectionMoreThanTheFamiliesGatedAtTheSend() {
            // View distance 2 chunks is 32 blocks — the same radius ServerLevel.sendParticles fixes for the ordinary
            // particle family, which does not get the second section.
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
            // ServerLevel.sendParticles offers within 32 blocks, or 512 when the sender overrides the limiter.
            assertBound(PacketReach.PARTICLE, 32.0 + MIRROR_GAP_BLOCKS);
            assertBound(PacketReach.FORCED_PARTICLE, 512.0 + MIRROR_GAP_BLOCKS);
        }

        @Test
        void explosionsReachOneSectionPastTheirRadius() {
            // ServerLevel.explode sends the burst to everyone within 64 blocks of the centre.
            assertBound(PacketReach.EXPLOSION, 64.0 + MIRROR_GAP_BLOCKS);
        }

        @Test
        void soundsReachOneSectionPastTheRangeTheyCarry() {
            assertBound(PacketReach.sound(16.0F), 16.0 + MIRROR_GAP_BLOCKS);
            assertBound(PacketReach.sound(256.0F), 256.0 + MIRROR_GAP_BLOCKS);
        }
    }
}
