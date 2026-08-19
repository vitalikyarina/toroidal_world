package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.phys.Vec3;

@Mixin(ThrownTrident.class)
public class ThrownTridentMixin {
    @ModifyExpressionValue(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getEyePosition()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$ownerEyeThroughSeam(Vec3 ownerEye) {
        return SeamAim.nearestTo((ThrownTrident) (Object) this, ownerEye);
    }
}
