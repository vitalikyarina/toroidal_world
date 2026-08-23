package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(value = ConnectivityHandler.class, remap = false)
public class ConnectivityHandlerMixin {
    @ModifyExpressionValue(
            method = "tryToFormNewMultiOfWidth",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/BlockEntity;getBlockPos()Lnet/minecraft/core/BlockPos;",
                    ordinal = 1))
    private static BlockPos toroidal$foldControllerIntoScanFrame(BlockPos conPos,
            @Local(argsOnly = true) BlockEntity be) {
        return CreateSeamFold.foldPosition(be.getLevel(), be.getBlockPos(), conPos);
    }

    @ModifyExpressionValue(
            method = "formMulti(Lnet/minecraft/world/level/block/entity/BlockEntityType;Lnet/minecraft/world/level/BlockGetter;Lcom/simibubi/create/api/connectivity/ConnectivityHandler$SearchCache;Ljava/util/List;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;relative(Lnet/minecraft/core/Direction;)Lnet/minecraft/core/BlockPos;"))
    private static BlockPos toroidal$foldFrontierStepIntoBoundFrame(BlockPos next,
            @Local(argsOnly = true) BlockGetter level, @Local(ordinal = 0) int minX, @Local(ordinal = 2) int minZ) {
        // Create arms minX/minZ only for a Y-axis multi and leaves them at MIN_VALUE otherwise, where folding the
        // step toward them would overflow the delta and there is no horizontal bound to keep honest anyway.
        if (minX == Integer.MIN_VALUE || minZ == Integer.MIN_VALUE || !(level instanceof Level blockLevel)) {
            return next;
        }

        return CreateSeamFold.foldPosition(blockLevel, new BlockPos(minX, next.getY(), minZ), next);
    }
}
