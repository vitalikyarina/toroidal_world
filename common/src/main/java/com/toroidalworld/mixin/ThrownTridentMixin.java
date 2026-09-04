package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.phys.Vec3;

@Mixin(ThrownTrident.class)
public class ThrownTridentMixin {
    @ModifyExpressionValue(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = InjectionTargets.ENTITY_GET_EYE_POSITION))
    private Vec3 toroidal$ownerEyeThroughSeam(Vec3 ownerEye) {
        return SeamAim.nearestTo((ThrownTrident) (Object) this, ownerEye);
    }
}
