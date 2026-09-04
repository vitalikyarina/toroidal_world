package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.monster.breeze.Slide;
import net.minecraft.world.phys.Vec3;

@Mixin(Slide.class)
public class BreezeSlideMixin {
    @ModifyExpressionValue(
            method = "randomPointInMiddleCircle",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_SUBTRACT))
    private static Vec3 toroidal$middleCircleHeadingThroughSeam(Vec3 direction, @Local(argsOnly = true) Breeze breeze) {
        return SeamAim.foldDelta(breeze, direction);
    }
}
