package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.base.DirectionalShaftHalvesBlockEntity;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

// The second delta of the family, and the one the propagator's own fold cannot reach: getSourceFacing subtracts in its
// own class, from the stored source field rather than from a neighbour handed to it. Across the seam that field names
// a block a world away, the derived facing is the opposite side, and getAxisModifier reads it to decide which way each
// half of a gearbox turns — so every output comes out inverted, and a clutch, gearshift or sequenced gearshift
// compares an incoming face against it and answers backwards too.
//
// Folded at the read. The field itself stays canonical: handleRemoved and propagateMissingSource compare it with
// equals against real block positions, and it is resolved through Level.getBlockEntity, which already folds.
@Mixin(value = DirectionalShaftHalvesBlockEntity.class, remap = false)
public class DirectionalShaftHalvesBlockEntityMixin {
    @WrapOperation(
            method = "getSourceFacing",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;subtract(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$foldSourceDelta(BlockPos sourcePos, Vec3i anchorPos, Operation<BlockPos> original) {
        DirectionalShaftHalvesBlockEntity self = (DirectionalShaftHalvesBlockEntity) (Object) this;
        return CreateSeamFold.foldDelta(self.getLevel(), self.getBlockPos(), sourcePos,
                original.call(sourcePos, anchorPos));
    }
}
