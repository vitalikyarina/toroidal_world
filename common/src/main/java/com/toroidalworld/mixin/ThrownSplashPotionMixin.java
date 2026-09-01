package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldFold;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.phys.AABB;

@Mixin(ThrownSplashPotion.class)
public class ThrownSplashPotionMixin {
    @WrapOperation(
            method = "onHitAsPotion",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/AABB;distanceToSqr(Lnet/minecraft/world/phys/AABB;)D"))
    private double toroidal$splashReachThroughSeam(AABB burst, AABB victim, Operation<Double> original) {
        WorldFold transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        if (transformer == null) {
            return original.call(burst, victim);
        }

        return original.call(burst, transformer.foldBox(burst.getCenter(), victim).value());
    }
}
