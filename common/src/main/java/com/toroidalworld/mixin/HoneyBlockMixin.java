package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.HoneyBlock;

@Mixin(HoneyBlock.class)
public abstract class HoneyBlockMixin {
    @ModifyVariable(
            method = "isSlidingDown(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            argsOnly = true)
    private BlockPos toroidal$slideAgainstNearestCopy(BlockPos pos, @Local(argsOnly = true) Entity entity) {
        return SeamAim.nearestTo(entity, pos);
    }
}
