package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.phys.AABB;

// A splash reaches four blocks, and how much of its effect anyone gets is the gap between the burst and them. The gap is
// taken raw, and the burst now lands where the potion really struck — which for a target across the seam is a point past
// the bounds, while the target's own coordinates are wrapped to the far edge. The two read a whole world apart, the four
// block test fails, and the potion breaks with its particles for nothing at all.
//
// The victim's box becomes the copy nearest the burst and vanilla's own falloff runs on that. A target on this side
// folds to itself, so an ordinary splash keeps its exact radius and its exact potency.
//
// This is the box-to-box overload of the distance, whose only caller in the game is this one line — so the fold sits
// here rather than on the primitive, where it would be a rule with nothing else to govern.
@Mixin(ThrownSplashPotion.class)
public class ThrownSplashPotionMixin {
    @WrapOperation(
            method = "onHitAsPotion",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/AABB;distanceToSqr(Lnet/minecraft/world/phys/AABB;)D"))
    private double toroidal$splashReachThroughSeam(AABB burst, AABB victim, Operation<Double> original) {
        WorldLoopTransformer transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        if (transformer == null) {
            return original.call(burst, victim);
        }

        return original.call(burst, transformer.foldBoxToward(burst.getCenter(), victim));
    }
}
