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

// The whole direction-named kinetic family reads through one subtraction. getRotationSpeedModifier derives the
// direction between two blocks from it, and then every other question in the method is asked of that same diff: the
// axis alignment, both shaft lookups, the custom hook Create hands to the block entity, both large-cog tests and the
// small-cog branch. Across the seam the diff reads a world wide, Direction.getNearest names the opposite side, and a
// block that describes its shaft by a signed face — drill, encased fan, hand crank — is asked about its solid side
// and refuses. One fold here answers for all of them.
//
// The propagator is a plain class with a plain method, so the subtraction sits in the method body and is the only one
// there; no lambda hides it and no ordinal has to be counted.
@Mixin(value = RotationPropagator.class, remap = false)
public class RotationPropagatorMixin {
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
