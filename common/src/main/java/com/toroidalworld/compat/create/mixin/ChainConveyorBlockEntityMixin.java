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

// A chain conveyor's connections are not positions but relative offsets, and that is what decides where this fold goes.
// Every consumer of the set resolves an entry as worldPosition.offset(connection) and hands the result to
// Level.getBlockEntity, which already folds — the tick's package hand-off, the port export, the validity sweep, the
// propagation neighbours, the destroy pass. The same value is what goes to disk, what is sent to the client, and what
// the far conveyor is expected to hold mirrored as multiply(-1). A raw offset a world wide is wrong in every one of
// those, so the store is where it is folded.
//
// That is not the read-side rule the other mixins here state, and it does not contradict it either: what a fold at the
// read protects is the equals identity of a position compared against canonical positions elsewhere in Create, and no
// connection offset is ever compared that way. What must hold instead is that the two ends and every lookup key name
// one value — which is exactly what a raw store breaks.
//
// The price is paid below. propagateRotationTo does not use the delta the propagator folded for it; it subtracts again
// and asks the set, so its key has to be folded too or it misses a stored connection every time.
@Mixin(value = ChainConveyorBlockEntity.class, remap = false)
public class ChainConveyorBlockEntityMixin {
    @WrapOperation(
            method = "addConnectionTo",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;subtract(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$foldStoredConnection(BlockPos target, Vec3i anchorPos, Operation<BlockPos> original) {
        return toroidal$foldConnection(target, anchorPos, original);
    }

    // The same fold, or the lookup misses the entry the store just folded — removeConnectionTo reaches the set, the
    // stats map and the travelling packages by this one key.
    @WrapOperation(
            method = "removeConnectionTo",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;subtract(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$foldRemovedConnection(BlockPos target, Vec3i anchorPos, Operation<BlockPos> original) {
        return toroidal$foldConnection(target, anchorPos, original);
    }

    // Taken at the lookup rather than at the subtraction that feeds it, because the delta exists for nothing else — the
    // fold and the question it is folded for stay one statement.
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
