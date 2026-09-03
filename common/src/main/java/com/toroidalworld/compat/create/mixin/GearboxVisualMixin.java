package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.gearbox.GearboxVisual;
import com.toroidalworld.compat.create.CreateInvokeTargets;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(value = GearboxVisual.class, remap = false)
public abstract class GearboxVisualMixin {
    @WrapOperation(
            method = "updateSourceFacing",
            at = @At(value = "INVOKE",
                    target = CreateInvokeTargets.BLOCK_POS_SUBTRACT))
    private BlockPos toroidal$foldSourceDelta(BlockPos sourcePos, Vec3i anchorPos, Operation<BlockPos> original) {
        BlockEntity blockEntity = ((AbstractBlockEntityVisualAccessor) this).toroidal$blockEntity();
        return CreateSeamFold.foldDelta(blockEntity.getLevel(), blockEntity.getBlockPos(), sourcePos,
                original.call(sourcePos, anchorPos));
    }
}
