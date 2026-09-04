package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.entity.SeamRange;
import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.animal.Bee;

@Mixin(Bee.class)
public class BeeMixin {
    @ModifyVariable(method = "closerThan(Lnet/minecraft/core/BlockPos;I)Z", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$rememberedPosThroughSeam(BlockPos targetPos) {
        return SeamSteering.nearestCopy((Bee) (Object) this, targetPos);
    }

    @WrapOperation(
            method = "pathfindRandomlyTowards",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_DIST_MANHATTAN))
    private int toroidal$stepDistanceThroughSeam(BlockPos beePos, Vec3i targetPos, Operation<Integer> original) {
        return SeamRange.manhattan((Bee) (Object) this, beePos, targetPos);
    }
}
