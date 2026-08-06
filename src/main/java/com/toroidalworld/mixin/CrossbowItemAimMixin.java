package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;

// The pillager and the piglin own no shot arithmetic of their own: their performRangedAttack asks the crossbow to fire
// at whatever they are targeting, and the delta is worked out inside the item. So this is where their aim is decided,
// and the shooter is an argument rather than the mixed-in object.
//
// The item reads coordinates from two entities of the same declared type — the target it was handed and the shooter
// holding it — so each fold names the first of them, which is the target's; the shooter's own reading, and the ones the
// shot sound takes afterwards, are left alone. Fired without a target the crossbow follows the holder's view vector and
// reaches none of this.
@Mixin(CrossbowItem.class)
public class CrossbowItemAimMixin {
    @ModifyExpressionValue(
            method = "shootProjectile",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getX()D", ordinal = 0))
    private double toroidal$aimTargetX(double targetX, @Local(argsOnly = true, ordinal = 0) LivingEntity shooter) {
        return SeamAim.nearX(shooter, targetX);
    }

    @ModifyExpressionValue(
            method = "shootProjectile",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D", ordinal = 0))
    private double toroidal$aimTargetZ(double targetZ, @Local(argsOnly = true, ordinal = 0) LivingEntity shooter) {
        return SeamAim.nearZ(shooter, targetZ);
    }
}
