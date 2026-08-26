package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.entity.SeamInside;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateInsideMixin {
    @ModifyVariable(
            method = "entityInside(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/InsideBlockEffectApplier;Z)V",
            at = @At("HEAD"),
            argsOnly = true)
    private BlockPos toroidal$canonicalInsidePos(BlockPos pos, @Local(argsOnly = true) Level level) {
        return SeamInside.canonical(level, pos);
    }

    @ModifyVariable(
            method = "getEntityInsideCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("HEAD"),
            argsOnly = true)
    private BlockPos toroidal$canonicalShapePos(BlockPos pos, @Local(argsOnly = true) BlockGetter getter) {
        return getter instanceof Level level ? SeamInside.canonical(level, pos) : pos;
    }
}
