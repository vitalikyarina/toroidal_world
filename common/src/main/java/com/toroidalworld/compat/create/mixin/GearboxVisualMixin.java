package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.gearbox.GearboxVisual;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.entity.BlockEntity;

// The third delta of the kinetic family, and the first that never reaches the server: a gearbox draws its own two half
// shafts by subtracting the source position from its own, and neither of the two renderers that do it asks
// getSourceFacing for the answer. This is the one a player normally sees — the Flywheel visual runs wherever the
// backend supports visualization, and GearboxRendererMixin covers the fallback beside it.
//
// Folded by the bounds rather than by the level: the source crossed inside the block entity tag as an absolute server
// coordinate — nothing translates opaque NBT — while the position it is subtracted from is the client's mirror, which
// keeps counting past the seam. So the two part company for every gearbox in the world once the player has lapped it,
// not only for one straddling the seam.
@Mixin(value = GearboxVisual.class, remap = false)
public abstract class GearboxVisualMixin {
    @WrapOperation(
            method = "updateSourceFacing",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;subtract(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$foldSourceDelta(BlockPos sourcePos, Vec3i anchorPos, Operation<BlockPos> original) {
        BlockEntity blockEntity = ((AbstractBlockEntityVisualAccessor) this).toroidal$blockEntity();
        return CreateSeamFold.foldClientDelta(blockEntity.getLevel(), blockEntity.getBlockPos(), sourcePos,
                original.call(sourcePos, anchorPos));
    }
}
