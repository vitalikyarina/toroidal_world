package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.boss.enderdragon.phases.DragonStrafePlayerPhase;

// The strafe run does not shoot whenever it can: it builds a horizontal aim at the target, takes the angle between that
// and the way the dragon is facing, and fires only inside ten degrees. Across the seam that angle is not merely wrong
// but arbitrary — it measures toward a point a world away — so the run either never fires at a player it is circling,
// or fires into empty ground in the opposite direction. Past the gate the fireball's own direction is a second raw
// difference, taken from a point just in front of the head.
//
// Both fold on the target's position where the phase reads it. The dragon and its head are one frame — a part is placed
// from the body every tick — so folding the target around the dragon settles the aim and the shot together.
//
// The same method also stores the target as a flight destination, and that reading folds here as well; it is folded a
// second time where EnderDragonMixin reads it back. Folding a copy that is already nearest returns it untouched, so the
// second pass costs nothing and covers the case where the dragon crossed the seam between the two.
@Mixin(DragonStrafePlayerPhase.class)
public class DragonStrafePlayerPhaseMixin {
    @ModifyExpressionValue(
            method = "doServerTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getX()D"))
    private double toroidal$aimTargetX(double targetX) {
        return SeamAim.nearX(((DragonPhaseAccessor) this).toroidal$dragon(), targetX);
    }

    @ModifyExpressionValue(
            method = "doServerTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D"))
    private double toroidal$aimTargetZ(double targetZ) {
        return SeamAim.nearZ(((DragonPhaseAccessor) this).toroidal$dragon(), targetZ);
    }
}
