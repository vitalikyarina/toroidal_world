package com.toroidalworld.compat.sable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.junit.jupiter.api.Test;

import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.api.physics.object.box.BoxPhysicsObject;
import dev.ryanhcode.sable.companion.math.Pose3d;

import net.minecraft.world.phys.Vec3;

class SableBodyPoseTest {
    private static final Vector3dc POSITION = new Vector3d(250.0, 70.0, 3.0);
    private static final Vector3dc HALF_EXTENTS = new Vector3d(0.5);
    private static final double MASS = 1.0;
    private static final Vector3dc ANCHOR_TWO_BLOCKS_ALONG_X = new Vector3d(2.0, 0.0, 0.0);
    private static final Vec3 ANCHOR_AFTER_A_QUARTER_TURN = new Vec3(250.0, 70.0, 1.0);
    private static final double EPSILON = 1.0e-9;

    private static final class Stranger implements PhysicsPipelineBody {
        @Override
        public int getRuntimeId() {
            return NULL_RUNTIME_ID;
        }

        @Override
        public MassData getMassTracker() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isRemoved() {
            return false;
        }
    }

    private static BoxPhysicsObject boxTurnedAQuarter() {
        Pose3d pose = new Pose3d();
        pose.position().set(POSITION);
        pose.orientation().set(new Quaterniond().rotateY(Math.PI / 2.0));
        return new BoxPhysicsObject(pose, HALF_EXTENTS, MASS);
    }

    @Test
    void aBoxAnswersWithItsOwnPose() {
        BoxPhysicsObject box = boxTurnedAQuarter();
        assertSame(box.getPose(), SableBodyPose.of(box));
    }

    @Test
    void aBoxAnchorTurnsWithTheBoxAndLandsAtItsPosition() {
        Vec3 world = SableBodyPose.anchorInWorld(boxTurnedAQuarter(), ANCHOR_TWO_BLOCKS_ALONG_X);
        assertNotNull(world);
        assertEquals(ANCHOR_AFTER_A_QUARTER_TURN.x, world.x, EPSILON);
        assertEquals(ANCHOR_AFTER_A_QUARTER_TURN.y, world.y, EPSILON);
        assertEquals(ANCHOR_AFTER_A_QUARTER_TURN.z, world.z, EPSILON);
    }

    @Test
    void anUnknownBodyKindRefuses() {
        Stranger stranger = new Stranger();
        assertNull(SableBodyPose.of(stranger));
        assertNull(SableBodyPose.anchorInWorld(stranger, ANCHOR_TWO_BLOCKS_ALONG_X));
    }
}
