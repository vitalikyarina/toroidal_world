package com.toroidalworld.compat.sable.mixin;

import org.joml.Quaterniondc;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.compat.sable.SableMotorGoal;
import com.toroidalworld.compat.sable.SableMotorGoals;

@Mixin(targets = "dev.ryanhcode.sable.physics.impl.rapier.constraint.generic.RapierGenericConstraintHandle",
        remap = false)
public class RapierGenericConstraintHandleMixin {
    @Inject(method = "setFrame1", at = @At("HEAD"))
    private void toroidal$followStaticFrame(Vector3dc localPosition, Quaterniondc localOrientation,
            CallbackInfo callback) {
        SableMotorGoal goal = ((SableMotorGoals) (Object) this).toroidal$motorGoal();
        if (goal != null) {
            goal.staticFrame(localPosition, localOrientation);
        }
    }

    @Inject(method = "setFrame2", at = @At("HEAD"))
    private void toroidal$followBodyFrame(Vector3dc localPosition, Quaterniondc localOrientation,
            CallbackInfo callback) {
        SableMotorGoal goal = ((SableMotorGoals) (Object) this).toroidal$motorGoal();
        if (goal != null) {
            goal.bodyFrame(localPosition);
        }
    }
}
