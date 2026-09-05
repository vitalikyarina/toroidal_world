package com.toroidalworld.compat.sable.mixin;

import org.joml.Vector3d;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.toroidalworld.compat.sable.SableConstraintEdge;
import com.toroidalworld.compat.sable.SableConstraintEdges;
import com.toroidalworld.compat.sable.SableMotorGoal;
import com.toroidalworld.compat.sable.SableMotorGoals;

@Mixin(targets = "dev.ryanhcode.sable.physics.impl.rapier.constraint.RapierConstraintHandle", remap = false)
public class RapierConstraintHandleMixin implements SableMotorGoals, SableConstraintEdges {
    @Unique
    private @Nullable SableMotorGoal toroidal$motorGoal;

    @Unique
    private @Nullable SableConstraintEdge toroidal$constraintEdge;

    @Override
    public @Nullable SableMotorGoal toroidal$motorGoal() {
        return this.toroidal$motorGoal;
    }

    @Override
    public void toroidal$motorGoal(@Nullable SableMotorGoal goal) {
        this.toroidal$motorGoal = goal;
    }

    @Override
    public void toroidal$constraintEdge(@Nullable SableConstraintEdge edge) {
        this.toroidal$constraintEdge = edge;
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void toroidal$forgetConstraint(CallbackInfo callback) {
        SableConstraintEdge edge = this.toroidal$constraintEdge;
        if (edge != null) {
            edge.drop();
            this.toroidal$constraintEdge = null;
        }

        this.toroidal$motorGoal = null;
    }

    @WrapOperation(method = "setMotor",
            at = @At(value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/physics/impl/rapier/Rapier3D;setConstraintMotor(JJIDDDZD)V"))
    private void toroidal$seatMotorGoal(long sceneHandle, long handle, int axis, double target, double stiffness,
            double damping, boolean hasForceLimit, double maxForce, Operation<Void> original) {
        SableMotorGoal goal = this.toroidal$motorGoal;
        if (goal == null || !goal.record(axis, target, stiffness, damping, hasForceLimit, maxForce)) {
            original.call(sceneHandle, handle, axis, target, stiffness, damping, hasForceLimit, maxForce);
            return;
        }

        Vector3d correction = goal.seatCorrection();
        if (correction == null) {
            original.call(sceneHandle, handle, axis, target, stiffness, damping, hasForceLimit, maxForce);
            return;
        }

        for (int linear = 0; linear < SableMotorGoal.LINEAR_AXES; linear++) {
            original.call(sceneHandle, handle, linear, goal.target(linear) + correction.get(linear),
                    goal.stiffness(linear), goal.damping(linear), goal.forceLimited(linear),
                    goal.maxForce(linear));
        }
    }
}
