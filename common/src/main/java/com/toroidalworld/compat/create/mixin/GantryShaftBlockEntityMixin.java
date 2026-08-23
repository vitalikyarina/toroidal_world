package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlockEntity;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

@Mixin(value = GantryShaftBlockEntity.class, remap = false)
public class GantryShaftBlockEntityMixin {
    @WrapOperation(
            method = "isCustomConnection",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;subtract(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$foldCarriageDelta(BlockPos carriagePos, Vec3i shaftPos, Operation<BlockPos> original) {
        GantryShaftBlockEntity self = (GantryShaftBlockEntity) (Object) this;
        BlockPos rawDelta = original.call(carriagePos, shaftPos);
        return CreateSeamFold.foldDelta(self.getLevel(), self.getBlockPos(), carriagePos, rawDelta);
    }
}
