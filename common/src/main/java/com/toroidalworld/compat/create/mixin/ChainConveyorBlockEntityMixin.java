package com.toroidalworld.compat.create.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

@Mixin(value = ChainConveyorBlockEntity.class, remap = false)
public class ChainConveyorBlockEntityMixin {
    @WrapOperation(
            method = "addConnectionTo",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;subtract(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$foldStoredConnection(BlockPos target, Vec3i anchorPos, Operation<BlockPos> original) {
        return toroidal$foldConnection(target, anchorPos, original);
    }

    @WrapOperation(
            method = "removeConnectionTo",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;subtract(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$foldRemovedConnection(BlockPos target, Vec3i anchorPos, Operation<BlockPos> original) {
        return toroidal$foldConnection(target, anchorPos, original);
    }

    @WrapOperation(
            method = "propagateRotationTo",
            at = @At(value = "INVOKE", target = "Ljava/util/Set;contains(Ljava/lang/Object;)Z"))
    private boolean toroidal$foldPropagationKey(Set<BlockPos> connections, Object rawDelta, Operation<Boolean> original,
            KineticBlockEntity target) {
        ChainConveyorBlockEntity self = (ChainConveyorBlockEntity) (Object) this;
        BlockPos raw = (BlockPos) rawDelta;
        BlockPos folded = CreateSeamFold.foldDelta(self.getLevel(), self.getBlockPos(), target.getBlockPos(), raw);
        return original.call(connections, folded);
    }

    private BlockPos toroidal$foldConnection(BlockPos target, Vec3i anchorPos, Operation<BlockPos> original) {
        ChainConveyorBlockEntity self = (ChainConveyorBlockEntity) (Object) this;
        return CreateSeamFold.foldDelta(self.getLevel(), self.getBlockPos(), target,
                original.call(target, anchorPos));
    }
}
