package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.entity.SeamRange;
import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.bee.Bee;

@Mixin(Bee.class)
public class BeeMixin {
    @ModifyVariable(method = "closerThan(Lnet/minecraft/core/BlockPos;I)Z", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$rememberedPosThroughSeam(BlockPos targetPos) {
        return SeamSteering.nearestCopy((Bee) (Object) this, targetPos);
    }

    @ModifyExpressionValue(
            method = "pathfindRandomlyTowards",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;distManhattan(Lnet/minecraft/core/Vec3i;)I"))
    private int toroidal$stepDistanceThroughSeam(int distance, @Local(argsOnly = true) BlockPos targetPos) {
        Bee bee = (Bee) (Object) this;
        return SeamRange.manhattan(bee, bee.blockPosition(), targetPos);
    }
}
