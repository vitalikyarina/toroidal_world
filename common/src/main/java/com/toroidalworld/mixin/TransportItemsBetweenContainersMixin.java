package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.entity.SeamAim;
import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

@Mixin(TransportItemsBetweenContainers.class)
public class TransportItemsBetweenContainersMixin {
    @ModifyExpressionValue(
            method = "getTransportTarget",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/ChestBlockEntity;getBlockPos()Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$candidateChestThroughSeam(BlockPos chestPos, @Local(argsOnly = true) PathfinderMob body) {
        return SeamSteering.nearestCopy(body, chestPos);
    }

    @ModifyExpressionValue(
            method = "isTargetValidToPick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/BlockEntity;getBlockPos()Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$searchAreaPosThroughSeam(BlockPos chestPos, @Local(argsOnly = true) PathfinderMob body) {
        return SeamSteering.nearestCopy(body, chestPos);
    }

    @ModifyExpressionValue(
            method = "isWithinTargetDistance",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;move(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/AABB;"))
    private AABB toroidal$reachBoxThroughSeam(AABB targetBox, @Local(argsOnly = true) PathfinderMob body,
            @Local(argsOnly = true) Vec3 fromPos) {
        WorldFold transformer = ((TransformerSource) body).toroidal$wrappedTransformer();
        if (transformer == null) {
            return targetBox;
        }

        return transformer.foldBox(fromPos, targetBox).value();
    }

    @ModifyExpressionValue(
            method = "canSeeAnyTargetSide",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_AT_CENTER_OF))
    private Vec3 toroidal$sightCentreThroughSeam(Vec3 centre, @Local(argsOnly = true) PathfinderMob body) {
        return SeamAim.nearestTo(body, centre);
    }

    @ModifyExpressionValue(
            method = "lambda$canSeeAnyTargetSide$1",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;"))
    private static BlockHitResult toroidal$sightHitThroughSeam(BlockHitResult hit,
            @Local(argsOnly = true) PathfinderMob body) {
        WorldFold transformer = ((TransformerSource) body).toroidal$wrappedTransformer();
        if (transformer == null) {
            return hit;
        }

        BlockPos hitPos = hit.getBlockPos();
        BlockPos wrapped = transformer.fold(hitPos);
        return wrapped == hitPos ? hit : hit.withPosition(wrapped);
    }
}
