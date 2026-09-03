package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.logistics.packagePort.PackagePortBlockEntity;
import com.simibubi.create.content.logistics.packagePort.PackagePortTarget;
import com.toroidalworld.compat.create.CreateInvokeTargets;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

@Mixin(value = PackagePortTarget.ChainConveyorFrogportTarget.class, remap = false)
public class ChainConveyorFrogportTargetMixin {
    @WrapOperation(method = "register",
            at = @At(value = "INVOKE",
                    target = CreateInvokeTargets.BLOCK_POS_SUBTRACT))
    private BlockPos toroidal$foldPortDelta(BlockPos conveyor, Vec3i port, Operation<BlockPos> original,
            PackagePortBlockEntity ppbe, LevelAccessor levelAccessor, BlockPos portPos) {
        if (!(levelAccessor instanceof Level level) || !(port instanceof BlockPos anchor)) {
            return original.call(conveyor, port);
        }

        return original.call(CreateSeamFold.nearestCopy(level, anchor, conveyor), port);
    }
}
