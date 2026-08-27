package com.toroidalworld.compat.sable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.shape.FlatShape;
import com.toroidalworld.options.WorldLoopBounds;

import net.minecraft.world.phys.Vec3;

class SeamFrameTest {
    private static final int HALF_WIDTH_CHUNKS = 16;
    private static final int WIDTH_BLOCKS = HALF_WIDTH_CHUNKS * 2 * 16;
    private static final int NO_SKEW = 0;
    private static final WorldFold FOLD = WorldFolds.of(FlatShape.latticeTorus(
            new WorldLoopBounds(-HALF_WIDTH_CHUNKS, HALF_WIDTH_CHUNKS, -HALF_WIDTH_CHUNKS, HALF_WIDTH_CHUNKS), NO_SKEW));

    private static final Vector3dc POSE_NEAR_SEAM = new Vector3d(254.5, 70.0, 3.5);
    private static final Vec3 ENTITY_ON_FAR_HALF = new Vec3(-253.5, 71.0, 3.5);
    private static final Vec3 ENTITY_ON_NEAR_HALF = new Vec3(252.0, 71.0, 3.5);

    @Test
    void unboundPoseKeepsItsPlace() {
        assertTrue(SeamFrame.isNoShift(SeamFrame.shiftOf(POSE_NEAR_SEAM)));
        assertFalse(SeamFrame.isBound());
    }

    @Test
    void entityOnTheFarHalfPullsThePoseOneLapBack() {
        Vector3dc shift = SeamFrame.with(FOLD, () -> ENTITY_ON_FAR_HALF, () -> SeamFrame.shiftOf(POSE_NEAR_SEAM));
        assertEquals(-WIDTH_BLOCKS, shift.x(), 0.0);
        assertEquals(0.0, shift.y(), 0.0);
        assertEquals(0.0, shift.z(), 0.0);
    }

    @Test
    void entityOnTheNearHalfLeavesThePoseAlone() {
        Vector3dc shift = SeamFrame.with(FOLD, () -> ENTITY_ON_NEAR_HALF, () -> SeamFrame.shiftOf(POSE_NEAR_SEAM));
        assertTrue(SeamFrame.isNoShift(shift));
    }

    @Test
    void theBindingIsScopedAndRestoresTheOuterOne() {
        SeamFrame.with(FOLD, () -> ENTITY_ON_FAR_HALF, () -> {
            Vector3dc inner = SeamFrame.with(FOLD, () -> ENTITY_ON_NEAR_HALF, () -> SeamFrame.shiftOf(POSE_NEAR_SEAM));
            assertTrue(SeamFrame.isNoShift(inner));
            assertEquals(-WIDTH_BLOCKS, SeamFrame.shiftOf(POSE_NEAR_SEAM).x(), 0.0);
            return null;
        });
        assertFalse(SeamFrame.isBound());
    }

    @Test
    void anUnwrappedLevelBindsNothing() {
        Vector3dc shift = SeamFrame.with((WorldFold) null, () -> ENTITY_ON_FAR_HALF, () -> SeamFrame.shiftOf(POSE_NEAR_SEAM));
        assertTrue(SeamFrame.isNoShift(shift));
    }
}
