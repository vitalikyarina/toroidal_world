package com.toroidalworld.compat.create.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.foundation.utility.RaycastHelper;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

@Mixin(value = RaycastHelper.class, remap = false)
public class RaycastHelperMixin {
    @ModifyReturnValue(method = "rayTraceRange", at = @At("RETURN"))
    private static @Nullable BlockHitResult toroidal$canonicaliseHit(@Nullable BlockHitResult hit, Level level) {
        if (hit == null || level.isClientSide()) {
            return hit;
        }

        return CreateSeamFold.canonical(level, hit);
    }
}
