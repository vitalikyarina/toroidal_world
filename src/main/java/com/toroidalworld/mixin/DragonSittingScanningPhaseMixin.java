package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.boss.enderdragon.phases.DragonSittingScanningPhase;

// A perched dragon spends its scan turning to face whoever came close, and then breathes at whatever it ended up facing
// — the flaming phase that follows takes its direction from the head's own look, not from the target. So a raw reading
// here does not merely misjudge an angle, it decides where the fire goes: across the seam the atan2 turns the dragon
// away from the player and it breathes into empty ground twenty-five ticks later.
//
// The aim cone above the turn is raw for the same reason and reads a meaningless angle, so the turn fires on ticks it
// should not and holds still on ticks it should turn.
//
// The third reading hands a charge destination to the next phase; it folds here and again where EnderDragonMixin reads
// the flight target back, which is a no-op the second time and covers a dragon that crossed the seam between the two.
@Mixin(DragonSittingScanningPhase.class)
public class DragonSittingScanningPhaseMixin {
    @ModifyExpressionValue(
            method = "doServerTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getX()D"))
    private double toroidal$scanTargetX(double targetX) {
        return SeamAim.nearX(((DragonPhaseAccessor) this).toroidal$dragon(), targetX);
    }

    @ModifyExpressionValue(
            method = "doServerTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D"))
    private double toroidal$scanTargetZ(double targetZ) {
        return SeamAim.nearZ(((DragonPhaseAccessor) this).toroidal$dragon(), targetZ);
    }
}
