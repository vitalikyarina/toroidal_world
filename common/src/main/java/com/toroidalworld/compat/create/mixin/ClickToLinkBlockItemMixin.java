package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.redstone.displayLink.ClickToLinkBlockItem;
import com.toroidalworld.compat.create.CreateInvokeTargets;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.context.UseOnContext;

@Mixin(value = ClickToLinkBlockItem.class, remap = false)
public class ClickToLinkBlockItemMixin {
    @WrapOperation(method = "useOn",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerThan(Lnet/minecraft/core/Vec3i;D)Z"))
    private boolean toroidal$foldSelectionReach(BlockPos selected, Vec3i placed, double maxDistance,
            Operation<Boolean> original, UseOnContext context) {
        if (!(placed instanceof BlockPos anchor)) {
            return original.call(selected, placed, maxDistance);
        }

        BlockPos folded = CreateSeamFold.nearestCopy(context.getLevel(), anchor, selected);
        return original.call(folded, placed, maxDistance);
    }

    @WrapOperation(method = "useOn",
            at = @At(value = "INVOKE",
                    target = CreateInvokeTargets.BLOCK_POS_SUBTRACT))
    private BlockPos toroidal$foldSelectionOffset(BlockPos selected, Vec3i placed, Operation<BlockPos> original,
            UseOnContext context) {
        BlockPos raw = original.call(selected, placed);
        if (!(placed instanceof BlockPos anchor)) {
            return raw;
        }

        return CreateSeamFold.foldDelta(context.getLevel(), anchor, selected, raw);
    }
}
