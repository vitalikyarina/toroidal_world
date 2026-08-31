package com.toroidalworld.compat.aeronautics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.toroidalworld.compat.aeronautics.RopeSeamFrame;

import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;

import net.minecraft.world.phys.Vec3;

@Mixin(value = RopeStrandHolderBehavior.class, remap = false)
public class RopeStrandHolderBehaviorMixin {
    private static final String PROJECT_OUT_OF_SUB_LEVEL =
            "Ldev/ryanhcode/sable/ActiveSableCompanion;projectOutOfSubLevel("
                    + "Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/Vec3;)"
                    + "Lnet/minecraft/world/phys/Vec3;";

    @ModifyExpressionValue(
            method = "createRope",
            at = @At(value = "INVOKE", ordinal = 0, target = PROJECT_OUT_OF_SUB_LEVEL))
    private Vec3 toroidal$seatRopeStart(Vec3 start, @Local(argsOnly = true) RopeStrandHolderBehavior target) {
        return RopeSeamFrame.seatStart((RopeStrandHolderBehavior) (Object) this, target, start);
    }

    @ModifyExpressionValue(
            method = "createRope",
            at = @At(value = "INVOKE", ordinal = 1, target = PROJECT_OUT_OF_SUB_LEVEL))
    private Vec3 toroidal$seatRopeTarget(Vec3 point, @Local(argsOnly = true) RopeStrandHolderBehavior target) {
        return RopeSeamFrame.seatTarget((RopeStrandHolderBehavior) (Object) this, target, point);
    }
}
