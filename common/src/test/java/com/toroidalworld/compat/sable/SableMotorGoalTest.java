package com.toroidalworld.compat.sable;

import static com.toroidalworld.compat.CompatFoldFixture.PER_AXIS;
import static com.toroidalworld.compat.CompatFoldFixture.WORLD_BLOCKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.junit.jupiter.api.Test;

import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.RotaryConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.object.box.BoxPhysicsObject;
import dev.ryanhcode.sable.companion.math.Pose3d;

import net.minecraft.world.phys.Vec3;

class SableMotorGoalTest {
    private static final Vector3dc ORIGIN = new Vector3d();
    private static final Vector3dc UP = new Vector3d(0.0, 1.0, 0.0);
    private static final Quaterniondc IDENTITY = new Quaterniond();
    private static final Quaterniondc QUARTER_TURN = new Quaterniond().rotateY(Math.PI / 2.0);
    private static final Vector3dc HALF_EXTENTS = new Vector3d(0.5);
    private static final double MASS = 1.0;
    private static final double HEIGHT = 70.0;
    private static final Vector3dc GOAL_NEAR_THE_SEAM = new Vector3d(250.0, HEIGHT, 3.0);
    private static final Vector3dc BODY_ACROSS_THE_SEAM = new Vector3d(-250.0, HEIGHT, 3.0);
    private static final Vector3dc BODY_ON_THE_SAME_HALF = new Vector3d(240.0, HEIGHT, 3.0);
    private static final Vector3dc ONE_LAP_BACK = new Vector3d(-WORLD_BLOCKS, 0.0, 0.0);
    private static final double STIFFNESS = 10.0;
    private static final double DAMPING = 2.0;
    private static final double MAX_FORCE = 100.0;
    private static final int ANGULAR_ORDINAL = SableMotorGoal.LINEAR_AXES;
    private static final double EPSILON = 1.0e-9;

    private static final class LiveBox extends BoxPhysicsObject {
        private boolean dropped;

        LiveBox(Vector3dc position) {
            super(poseAt(position), HALF_EXTENTS, MASS);
        }

        @Override
        public boolean isRemoved() {
            return this.dropped;
        }

        void drop() {
            this.dropped = true;
        }
    }

    private static Pose3d poseAt(Vector3dc position) {
        Pose3d pose = new Pose3d();
        pose.position().set(position);
        return pose;
    }

    private static SableMotorGoal goalOver(LiveBox body, Vector3dc staticAnchor, Quaterniondc orientation) {
        SableMotorGoal goal = SableMotorGoal.of(PER_AXIS, body,
                new FixedConstraintConfiguration(staticAnchor, ORIGIN, orientation));
        assertNotNull(goal);
        return goal;
    }

    private static void aimAt(SableMotorGoal goal, Vector3dc target) {
        for (int axis = 0; axis < SableMotorGoal.LINEAR_AXES; axis++) {
            assertTrue(goal.record(axis, target.get(axis), STIFFNESS, DAMPING, false, MAX_FORCE));
        }
    }

    @Test
    void recordRefusesAnOrdinalOutsideTheLinearAxes() {
        SableMotorGoal goal = goalOver(new LiveBox(BODY_ACROSS_THE_SEAM), ORIGIN, IDENTITY);

        assertFalse(goal.record(ANGULAR_ORDINAL, 0.0, STIFFNESS, DAMPING, false, MAX_FORCE));
        assertFalse(goal.record(-1, 0.0, STIFFNESS, DAMPING, false, MAX_FORCE));
    }

    @Test
    void recordKeepsEveryFieldOfTheAxis() {
        SableMotorGoal goal = goalOver(new LiveBox(BODY_ACROSS_THE_SEAM), ORIGIN, IDENTITY);

        assertTrue(goal.record(1, HEIGHT, STIFFNESS, DAMPING, true, MAX_FORCE));

        assertEquals(HEIGHT, goal.target(1), 0.0);
        assertEquals(STIFFNESS, goal.stiffness(1), 0.0);
        assertEquals(DAMPING, goal.damping(1), 0.0);
        assertTrue(goal.forceLimited(1));
        assertEquals(MAX_FORCE, goal.maxForce(1), 0.0);
    }

    @Test
    void theCorrectionWaitsForAllThreeLinearAxes() {
        SableMotorGoal goal = goalOver(new LiveBox(BODY_ACROSS_THE_SEAM), ORIGIN, IDENTITY);

        goal.record(0, GOAL_NEAR_THE_SEAM.x(), STIFFNESS, DAMPING, false, MAX_FORCE);
        goal.record(1, GOAL_NEAR_THE_SEAM.y(), STIFFNESS, DAMPING, false, MAX_FORCE);
        assertNull(goal.seatCorrection());

        goal.record(2, GOAL_NEAR_THE_SEAM.z(), STIFFNESS, DAMPING, false, MAX_FORCE);
        assertNotNull(goal.seatCorrection());
    }

    @Test
    void aBodyOnTheSameHalfNeedsNoCorrection() {
        SableMotorGoal goal = goalOver(new LiveBox(BODY_ON_THE_SAME_HALF), ORIGIN, IDENTITY);
        aimAt(goal, GOAL_NEAR_THE_SEAM);

        assertNull(goal.seatCorrection());
    }

    @Test
    void aBodyAcrossTheSeamPullsTheGoalOneLapOver() {
        SableMotorGoal goal = goalOver(new LiveBox(BODY_ACROSS_THE_SEAM), ORIGIN, IDENTITY);
        aimAt(goal, GOAL_NEAR_THE_SEAM);

        assertEquals(ONE_LAP_BACK, goal.seatCorrection());
    }

    @Test
    void theCorrectionComesBackInTheConstraintFrame() {
        Vector3dc staticAnchor = new Vector3d(0.0, HEIGHT, 0.0);
        Vector3dc localTarget = QUARTER_TURN.transformInverse(new Vector3d(GOAL_NEAR_THE_SEAM).sub(staticAnchor));
        SableMotorGoal goal = goalOver(new LiveBox(BODY_ACROSS_THE_SEAM), staticAnchor, QUARTER_TURN);
        aimAt(goal, localTarget);

        Vector3d correction = goal.seatCorrection();

        assertNotNull(correction);
        Vector3d reseated = QUARTER_TURN.transform(correction.add(localTarget)).add(staticAnchor);
        Vec3 expected = PER_AXIS.nearestCopy(toVec3(BODY_ACROSS_THE_SEAM), toVec3(GOAL_NEAR_THE_SEAM));
        assertEquals(expected.x, reseated.x, EPSILON);
        assertEquals(expected.y, reseated.y, EPSILON);
        assertEquals(expected.z, reseated.z, EPSILON);
    }

    @Test
    void aMovedStaticFrameMovesTheGoalWithIt() {
        SableMotorGoal goal = goalOver(new LiveBox(BODY_ACROSS_THE_SEAM), ORIGIN, IDENTITY);
        aimAt(goal, GOAL_NEAR_THE_SEAM);
        assertNotNull(goal.seatCorrection());

        goal.staticFrame(ONE_LAP_BACK, IDENTITY);

        assertNull(goal.seatCorrection());
    }

    @Test
    void aRemovedBodyGetsNoCorrection() {
        LiveBox body = new LiveBox(BODY_ACROSS_THE_SEAM);
        SableMotorGoal goal = goalOver(body, ORIGIN, IDENTITY);
        aimAt(goal, GOAL_NEAR_THE_SEAM);

        body.drop();

        assertNull(goal.seatCorrection());
    }

    @Test
    void aRotaryConstraintCarriesNoGoal() {
        assertNull(SableMotorGoal.of(PER_AXIS, new LiveBox(BODY_ACROSS_THE_SEAM),
                new RotaryConstraintConfiguration(ORIGIN, ORIGIN, UP, UP)));
    }

    private static Vec3 toVec3(Vector3dc vector) {
        return new Vec3(vector.x(), vector.y(), vector.z());
    }
}
