package com.toroidalworld.compat.aeronautics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.toroidalworld.compat.aeronautics.RopeSeamFrame;

import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;

import net.minecraft.world.phys.Vec3;

@Mixin(value = RopeStrandHolderBehavior.class, remap = false)
public class RopeStrandHolderBehaviorMixin {
    @ModifyExpressionValue(
            method = "createRope",
            at = @At(value = "INVOKE", ordinal = 1,
                    target = "Ldev/ryanhcode/sable/ActiveSableCompanion;projectOutOfSubLevel("
                            + "Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/Vec3;)"
                            + "Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$seatRopeTarget(Vec3 target) {
        return RopeSeamFrame.seatTarget((RopeStrandHolderBehavior) (Object) this, target);
    }
}
