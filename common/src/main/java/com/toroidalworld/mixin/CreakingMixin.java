package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.phys.Vec3;

@Mixin(Creaking.class)
public class CreakingMixin {
    @ModifyExpressionValue(
            method = "playerIsStuckInYou",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getEyePosition()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$stuckEyeThroughSeam(Vec3 eyePosition) {
        return SeamSteering.nearestCopy((Creaking) (Object) this, eyePosition);
    }
}
