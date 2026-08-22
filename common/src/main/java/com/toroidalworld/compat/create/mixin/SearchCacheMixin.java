package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.sugar.Local;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(targets = "com.simibubi.create.api.connectivity.ConnectivityHandler$SearchCache", remap = false)
public class SearchCacheMixin {
    @ModifyVariable(
            method = "getOrCache(Lnet/minecraft/world/level/block/entity/BlockEntityType;"
                    + "Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Ljava/util/Optional;",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private BlockPos toroidal$canonicalLookupKey(BlockPos pos, @Local(argsOnly = true) BlockGetter level) {
        return CreateSeamFold.canonical(level instanceof Level blockLevel ? blockLevel : null, pos);
    }

    @ModifyVariable(
            method = "put(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private BlockPos toroidal$canonicalStoreKey(BlockPos pos, @Local(argsOnly = true) BlockEntity target) {
        return CreateSeamFold.canonical(target.getLevel(), pos);
    }
}
