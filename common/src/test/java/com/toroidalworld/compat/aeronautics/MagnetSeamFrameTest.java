package com.toroidalworld.compat.aeronautics;

import static com.toroidalworld.compat.CompatFoldFixture.PER_AXIS;
import static com.toroidalworld.compat.CompatFoldFixture.WORLD_BLOCKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFolds;

import net.minecraft.world.phys.Vec3;

class MagnetSeamFrameTest {
    private static final double LOW_TIP_Y = 64.0;
    private static final double HIGH_TIP_Y = 80.0;
    private static final double MAGNET_Y = 70.0;
    private static final double ANCHOR_Y = 64.0;
    private static final double Z = 10.0;

    private static Vec3 lowTip(double x) {
        return new Vec3(x, LOW_TIP_Y, Z);
    }

    private static Vec3 highTip(double x) {
        return new Vec3(x, HIGH_TIP_Y, Z);
    }

    private static Vector3d averageOf(Vec3 tip1, Vec3 tip2) {
        return new Vector3d(tip1.x + tip2.x, tip1.y + tip2.y, tip1.z + tip2.z).div(2.0);
    }

    private static Vector3d seamMidpoint() {
        return new Vector3d(-WORLD_BLOCKS / 2.0, (LOW_TIP_Y + HIGH_TIP_Y) / 2.0, Z);
    }

    private static Vector3d magnet(double x) {
        return new Vector3d(x, MAGNET_Y, Z);
    }

    private static Vec3 anchor(double x) {
        return new Vec3(x, ANCHOR_Y, Z);
    }

    @Test
    void theMidpointIsTheSameWhicheverTipIsTheAnchor() {
        Vec3 near = lowTip(250.0);
        Vec3 far = highTip(-250.0);
        Vector3d oneWay = averageOf(near, far);
        Vector3d theOther = averageOf(far, near);

        MagnetSeamFrame.midpoint(PER_AXIS, near, far, oneWay);
        MagnetSeamFrame.midpoint(PER_AXIS, far, near, theOther);

        assertEquals(seamMidpoint(), oneWay);
        assertEquals(oneWay, theOther);
    }

    @Test
    void theMidpointIsWrittenIntoTheAverageItWasGiven() {
        Vec3 near = lowTip(250.0);
        Vec3 far = highTip(-250.0);
        Vector3d average = averageOf(near, far);

        assertSame(average, MagnetSeamFrame.midpoint(PER_AXIS, near, far, average));
        assertEquals(seamMidpoint(), average);
    }

    @Test
    void theEarlyOutKeepsTheHeightTheFoldWouldHaveWritten() {
        Vec3 low = lowTip(100.0);
        Vec3 high = highTip(120.0);
        Vector3d average = averageOf(low, high);

        assertSame(average, MagnetSeamFrame.midpoint(PER_AXIS, low, high, average));
        assertEquals(averageOf(low, high), average);
    }

    @Test
    void anUnwrappedWorldGivesTheAverageBackByIdentity() {
        Vec3 near = lowTip(250.0);
        Vec3 far = highTip(-250.0);
        Vector3d average = averageOf(near, far);

        assertSame(average, MagnetSeamFrame.midpoint(null, near, far, average));
        assertSame(average, MagnetSeamFrame.midpoint(WorldFolds.NOOP, near, far, average));
        assertEquals(averageOf(near, far), average);
    }

    @Test
    void aMagnetAcrossTheSeamIsSeatedOnTheBlocksSide() {
        assertEquals(magnet(-250.0 + WORLD_BLOCKS),
                MagnetSeamFrame.seatNearbyMagnet(PER_AXIS, anchor(250.0), magnet(-250.0)));
        assertEquals(magnet(250.0 - WORLD_BLOCKS),
                MagnetSeamFrame.seatNearbyMagnet(PER_AXIS, anchor(-250.0), magnet(250.0)));
    }

    @Test
    void aSeatedMagnetIsANewVectorAndLeavesTheArgumentWhereItWas() {
        Vector3d across = magnet(-250.0);
        Object seated = MagnetSeamFrame.seatNearbyMagnet(PER_AXIS, anchor(250.0), across);

        assertNotSame(across, seated);
        assertEquals(magnet(-250.0), across);
    }

    @Test
    void aMagnetOnTheBlocksOwnSideComesBackByIdentity() {
        Vector3d near = magnet(254.0);

        assertSame(near, MagnetSeamFrame.seatNearbyMagnet(PER_AXIS, anchor(250.0), near));
    }

    @Test
    void whatIsNotAPositionAndAnUnwrappedWorldComeBackByIdentity() {
        Object notAPosition = new Object();
        Vector3d across = magnet(-250.0);

        assertSame(notAPosition, MagnetSeamFrame.seatNearbyMagnet(PER_AXIS, anchor(250.0), notAPosition));
        assertSame(across, MagnetSeamFrame.seatNearbyMagnet(null, anchor(250.0), across));
        assertSame(across, MagnetSeamFrame.seatNearbyMagnet(WorldFolds.NOOP, anchor(250.0), across));
    }
}
