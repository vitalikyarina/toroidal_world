package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.monster.Ghast;

// A ghast does not turn through a look control: its own goal writes its yaw from a raw difference to its target. That
// is not only how it faces — the fireball leaves from a point four blocks along its view vector, so a yaw pointing the
// long way round the world also puts the shot's origin on the wrong side of the ghast.
//
// The goal reads the target as a LivingEntity and itself as a Ghast, so the two coordinate reads that make the
// difference carry different invoke owners in the bytecode and the target's are named on their own. The branch taken
// when there is no target steers off the ghast's own delta movement and needs nothing.
@Mixin(targets = "net.minecraft.world.entity.monster.Ghast$GhastLookGoal")
public class GhastFacingAimMixin {
    @Shadow
    @Final
    private Ghast ghast;

    @ModifyExpressionValue(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getX()D"))
    private double toroidal$aimTargetX(double targetX) {
        return SeamAim.nearX(this.ghast, targetX);
    }

    @ModifyExpressionValue(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D"))
    private double toroidal$aimTargetZ(double targetZ) {
        return SeamAim.nearZ(this.ghast, targetZ);
    }
}
