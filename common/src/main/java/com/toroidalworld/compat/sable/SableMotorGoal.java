package com.toroidalworld.compat.sable;

import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;

import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.FreeConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.RotaryConstraintConfiguration;

import net.minecraft.world.phys.Vec3;

public final class SableMotorGoal {
    public static final int LINEAR_AXES = ConstraintJointAxis.LINEAR.length;

    private static final int ALL_LINEAR_WRITTEN = (1 << LINEAR_AXES) - 1;

    private final WorldFold fold;
    private final PhysicsPipelineBody body;
    private final Vector3d staticAnchor = new Vector3d();
    private final Quaterniond staticOrientation = new Quaterniond();
    private final Vector3d bodyAnchor = new Vector3d();
    private final double[] targets = new double[LINEAR_AXES];
    private final double[] stiffness = new double[LINEAR_AXES];
    private final double[] damping = new double[LINEAR_AXES];
    private final boolean[] forceLimited = new boolean[LINEAR_AXES];
    private final double[] maxForce = new double[LINEAR_AXES];
    private int written;

    private SableMotorGoal(WorldFold fold, PhysicsPipelineBody body, Vector3dc staticAnchor,
            Quaterniondc staticOrientation, Vector3dc bodyAnchor) {
        this.fold = fold;
        this.body = body;
        this.staticAnchor.set(staticAnchor);
        this.staticOrientation.set(staticOrientation);
        this.bodyAnchor.set(bodyAnchor);
    }

    public static @Nullable SableMotorGoal of(WorldFold fold, PhysicsPipelineBody body,
            PhysicsConstraintConfiguration<?> configuration) {
        return switch (configuration) {
            case FixedConstraintConfiguration config -> new SableMotorGoal(fold, body, config.pos1(),
                    config.orientation(), config.pos2());
            case FreeConstraintConfiguration config -> new SableMotorGoal(fold, body, config.pos1(),
                    config.orientation(), config.pos2());
            case GenericConstraintConfiguration config -> new SableMotorGoal(fold, body, config.pos1(),
                    config.orientation1(), config.pos2());
            case RotaryConstraintConfiguration ignored -> null;
        };
    }

    public void staticFrame(Vector3dc anchor, Quaterniondc orientation) {
        this.staticAnchor.set(anchor);
        this.staticOrientation.set(orientation);
    }

    public void bodyFrame(Vector3dc anchor) {
        this.bodyAnchor.set(anchor);
    }

    public boolean record(int axis, double target, double stiffness, double damping, boolean hasForceLimit,
            double maxForce) {
        if (axis < 0 || axis >= LINEAR_AXES) {
            return false;
        }

        this.targets[axis] = target;
        this.stiffness[axis] = stiffness;
        this.damping[axis] = damping;
        this.forceLimited[axis] = hasForceLimit;
        this.maxForce[axis] = maxForce;
        this.written |= 1 << axis;
        return true;
    }

    public double target(int axis) {
        return this.targets[axis];
    }

    public double stiffness(int axis) {
        return this.stiffness[axis];
    }

    public double damping(int axis) {
        return this.damping[axis];
    }

    public boolean forceLimited(int axis) {
        return this.forceLimited[axis];
    }

    public double maxForce(int axis) {
        return this.maxForce[axis];
    }

    public @Nullable Vector3d seatCorrection() {
        if (this.written != ALL_LINEAR_WRITTEN || this.body.isRemoved()) {
            return null;
        }

        return SeamFrame.unbound(() -> {
            Vec3 anchor = SableBodyPose.anchorInWorld(this.body, this.bodyAnchor);
            if (anchor == null) {
                return null;
            }

            Vector3d goal = this.staticOrientation
                    .transform(new Vector3d(this.targets[0], this.targets[1], this.targets[2]))
                    .add(this.staticAnchor);
            Vec3 raw = new Vec3(goal.x, goal.y, goal.z);
            Vec3 seated = this.fold.nearestCopy(anchor, raw);
            if (seated.x == raw.x && seated.z == raw.z) {
                return null;
            }

            return this.staticOrientation.transformInverse(new Vector3d(seated.x - raw.x, 0.0, seated.z - raw.z));
        });
    }
}
