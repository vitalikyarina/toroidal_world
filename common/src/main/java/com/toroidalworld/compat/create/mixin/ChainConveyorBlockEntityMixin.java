package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

@Mixin(value = ChainConveyorBlockEntity.class, remap = false)
public class ChainConveyorBlockEntityMixin {
    @WrapOperation(
            method = {"addConnectionTo", "removeConnectionTo", "propagateRotationTo"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;subtract(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/core/BlockPos;"),
            require = 3,
            allow = 3)
    private BlockPos toroidal$foldConnectionDelta(BlockPos target, Vec3i anchorPos, Operation<BlockPos> original) {
        ChainConveyorBlockEntity self = (ChainConveyorBlockEntity) (Object) this;
        return CreateSeamFold.foldDelta(self.getLevel(), self.getBlockPos(), target,
                original.call(target, anchorPos));
    }
}
