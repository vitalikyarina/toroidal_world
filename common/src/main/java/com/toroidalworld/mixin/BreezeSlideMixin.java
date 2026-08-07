package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.monster.breeze.Slide;
import net.minecraft.world.phys.Vec3;

// Where the breeze puts itself when it is not backing away: a point on the line to its target, a few blocks short of it.
// The line is a raw difference between the two positions, and it is read twice — a length that sets how far along to
// stand, and a normalized heading that says which way. Through the seam the length is nearly the width of the world and
// the heading points the long way round, so the breeze takes its stance on the wrong side of what it is fighting.
//
// Folding the difference leaves both readings vanilla's own arithmetic, correct because their input now names the copy
// of the target the breeze is actually standing next to.
@Mixin(Slide.class)
public class BreezeSlideMixin {
    @ModifyExpressionValue(
            method = "randomPointInMiddleCircle",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 toroidal$middleCircleHeadingThroughSeam(Vec3 direction, @Local(argsOnly = true) Breeze breeze) {
        return SeamAim.foldDelta(breeze, direction);
    }
}
