package com.toroidalworld.mixin;

import java.util.Comparator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.FoldedOrder;
import com.toroidalworld.core.WorldFold;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.PrepareRamNearestTarget;
import net.minecraft.world.phys.Vec3;

@Mixin(PrepareRamNearestTarget.class)
public class PrepareRamNearestTargetMixin {
    @ModifyArg(
            method = "calculateRammingStartPosition",
            at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;sorted(Ljava/util/Comparator;)Ljava/util/stream/Stream;"),
            index = 0)
    private Comparator<BlockPos> toroidal$ramStartOrderThroughSeam(Comparator<BlockPos> original,
            @Local(argsOnly = true) PathfinderMob body) {
        WorldFold transformer = ((TransformerSource) body).toroidal$wrappedTransformer();
        if (transformer == null) {
            return original;
        }

        return FoldedOrder.around(original, transformer, body.blockPosition());
    }

    @ModifyExpressionValue(
            method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PathfinderMob;J)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/behavior/PrepareRamNearestTarget$RamCandidate;getStartPosition()Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$ramStartThroughSeam(BlockPos startPos, @Local(argsOnly = true) PathfinderMob body) {
        WorldFold transformer = ((TransformerSource) body).toroidal$wrappedTransformer();
        if (transformer == null) {
            return startPos;
        }

        return transformer.fold(startPos);
    }

    @WrapOperation(
            method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PathfinderMob;J)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/behavior/PrepareRamNearestTarget;getEdgeOfBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$ramEdgeThroughSeam(PrepareRamNearestTarget<?> self, BlockPos startRamPos, BlockPos targetPos,
            Operation<Vec3> original, @Local(argsOnly = true) PathfinderMob body) {
        WorldFold transformer = ((TransformerSource) body).toroidal$wrappedTransformer();
        if (transformer == null) {
            return original.call(self, startRamPos, targetPos);
        }

        return original.call(self, transformer.nearestCopy(targetPos, startRamPos), targetPos);
    }
}
