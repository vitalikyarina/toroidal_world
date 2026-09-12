package com.toroidalworld.compat.aeronautics.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.toroidalworld.core.WorldLoopAttachments;

import dev.simulated_team.simulated.content.navigation_targets.MapNavigationTarget;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(value = MapNavigationTarget.class, remap = false)
public class MapNavigationTargetMixin {
    @WrapOperation(
            method = "getNearestDecorationPos",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(DDD)D"),
            require = 2,
            expect = 2)
    private static double toroidal$distanceTheShortWayRound(Vec3 from, double x, double y, double z,
            Operation<Double> original, @Local(argsOnly = true) @Nullable Level level) {
        return WorldLoopAttachments.transformerOfReader(level).sqrDistance(from.x, from.y, from.z, x, y, z);
    }
}
