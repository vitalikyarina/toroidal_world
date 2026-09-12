package com.toroidalworld.engine.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MeasuredReachTest {
    private static final double PARTICLE_RADIUS = 32.0;

    private static final double SOUND_RADIUS = 16.0;

    @Test
    void nothingIsMeasuredOutsideASend() {
        assertEquals(MeasuredReach.UNMEASURED, MeasuredReach.blocks());
    }

    @Test
    void theRadiusStandsForTheLengthOfTheSend() {
        try (MeasuredReach ignored = MeasuredReach.measuring(PARTICLE_RADIUS)) {
            assertEquals(PARTICLE_RADIUS, MeasuredReach.blocks());
        }

        assertEquals(MeasuredReach.UNMEASURED, MeasuredReach.blocks());
    }

    @Test
    void anInnerSendGivesTheOuterRadiusBack() {
        try (MeasuredReach ignoredOuter = MeasuredReach.measuring(PARTICLE_RADIUS)) {
            try (MeasuredReach ignoredInner = MeasuredReach.measuring(SOUND_RADIUS)) {
                assertEquals(SOUND_RADIUS, MeasuredReach.blocks());
            }

            assertEquals(PARTICLE_RADIUS, MeasuredReach.blocks());
        }
    }
}
