package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

@Mixin(value = RotationPropagator.class, remap = false)
public class RotationPropagatorMixin {
    // isLargeCogToSpeedController accepts only the vertical one-block diff (0,-1,0) and Y never wraps, so the raw
    // subtracts feeding it from getConveyedSpeed and isConnected cannot change its answer — the family's seam
    // coverage ends here.
    @WrapOperation(
            method = "getRotationSpeedModifier",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;subtract(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/core/BlockPos;"))
    private static BlockPos toroidal$foldNeighbourDelta(BlockPos targetPos, Vec3i anchorPos,
            Operation<BlockPos> original, KineticBlockEntity from, KineticBlockEntity to) {
        return CreateSeamFold.foldDelta(from.getLevel(), from.getBlockPos(), targetPos,
                original.call(targetPos, anchorPos));
    }
}
