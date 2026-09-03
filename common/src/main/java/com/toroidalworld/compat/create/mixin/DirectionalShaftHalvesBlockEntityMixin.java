package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.base.DirectionalShaftHalvesBlockEntity;
import com.toroidalworld.compat.create.CreateInvokeTargets;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

@Mixin(value = DirectionalShaftHalvesBlockEntity.class, remap = false)
public class DirectionalShaftHalvesBlockEntityMixin {
    @WrapOperation(
            method = "getSourceFacing",
            at = @At(value = "INVOKE",
                    target = CreateInvokeTargets.BLOCK_POS_SUBTRACT))
    private BlockPos toroidal$foldSourceDelta(BlockPos sourcePos, Vec3i anchorPos, Operation<BlockPos> original) {
        DirectionalShaftHalvesBlockEntity self = (DirectionalShaftHalvesBlockEntity) (Object) this;
        return CreateSeamFold.foldDelta(self.getLevel(), self.getBlockPos(), sourcePos,
                original.call(sourcePos, anchorPos));
    }
}
