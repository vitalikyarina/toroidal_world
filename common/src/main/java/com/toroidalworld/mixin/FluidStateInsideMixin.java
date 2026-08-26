package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.entity.SeamInside;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;

@Mixin(FluidState.class)
public abstract class FluidStateInsideMixin {
    @ModifyVariable(
            method = "entityInside(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/InsideBlockEffectApplier;)V",
            at = @At("HEAD"),
            argsOnly = true)
    private BlockPos toroidal$canonicalInsidePos(BlockPos pos, @Local(argsOnly = true) Level level) {
        return SeamInside.canonical(level, pos);
    }
}
