package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.entity.SeamAim;
import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.Position;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetAwayFrom;
import net.minecraft.world.phys.Vec3;

@Mixin(SetWalkTargetAwayFrom.class)
public class SetWalkTargetAwayFromMixin {
    @WrapOperation(
            method = "*",
            require = 1,
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_CLOSER_THAN))
    private static boolean toroidal$avoidReachThroughSeam(Vec3 bodyPosition, Position avoidPosition, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) PathfinderMob body) {
        return SeamRange.closerThan(body, bodyPosition, avoidPosition, distance);
    }

    @WrapOperation(
            method = "*",
            require = 2,
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_SUBTRACT))
    private static Vec3 toroidal$avoidHeadingThroughSeam(Vec3 from, Vec3 to, Operation<Vec3> original,
            @Local(argsOnly = true) PathfinderMob body) {
        Vec3 vanilla = original.call(from, to);
        return SeamAim.foldDelta(body, vanilla);
    }
}
