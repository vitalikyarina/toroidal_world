package com.toroidalworld.compat.aeronautics;

import static com.toroidalworld.compat.CompatFoldFixture.PER_AXIS;
import static com.toroidalworld.compat.CompatFoldFixture.WORLD_BLOCKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFolds;

class LinkedReceiverSeamDeltaTest {
    private static final double RECEIVER_Y = 64.0;
    private static final double SENDER_Y = 70.0;
    private static final double RISE = SENDER_Y - RECEIVER_Y;
    private static final double Z = 10.0;

    private static Vector3d receiver(double x) {
        return new Vector3d(x, RECEIVER_Y, Z);
    }

    private static Vector3d sender(double x) {
        return new Vector3d(x, SENDER_Y, Z);
    }

    private static Vector3d relative(double x) {
        return new Vector3d(x, RISE, 0.0);
    }

    @Test
    void aSenderAcrossTheSeamGivesTheShortRelativePosition() {
        assertEquals(relative(-250.0 + WORLD_BLOCKS - 250.0),
                LinkedReceiverSeamDelta.fold(PER_AXIS, sender(-250.0), receiver(250.0)));
        assertEquals(relative(250.0 - WORLD_BLOCKS + 250.0),
                LinkedReceiverSeamDelta.fold(PER_AXIS, sender(250.0), receiver(-250.0)));
    }

    @Test
    void aSenderOnTheReceiversOwnSideGivesThePlainDifference() {
        assertEquals(relative(4.0), LinkedReceiverSeamDelta.fold(PER_AXIS, sender(254.0), receiver(250.0)));
    }

    @Test
    void theReceiverReadsTheResultBackOutOfTheVectorItPassed() {
        Vector3d target = sender(-250.0);

        assertSame(target, LinkedReceiverSeamDelta.fold(PER_AXIS, target, receiver(250.0)));
        assertEquals(relative(-250.0 + WORLD_BLOCKS - 250.0), target);
    }

    @Test
    void theHeightIsThePlainDifferenceHoweverLarge() {
        Vector3d farAbove = new Vector3d(254.0, RECEIVER_Y + WORLD_BLOCKS, Z);

        assertEquals(new Vector3d(4.0, WORLD_BLOCKS, 0.0),
                LinkedReceiverSeamDelta.fold(PER_AXIS, farAbove, receiver(250.0)));
    }

    @Test
    void anUnwrappedWorldGivesTheLongWayRound() {
        Vector3d target = sender(-250.0);

        assertSame(target, LinkedReceiverSeamDelta.fold(WorldFolds.NOOP, target, receiver(250.0)));
        assertEquals(relative(-500.0), target);
    }
}
