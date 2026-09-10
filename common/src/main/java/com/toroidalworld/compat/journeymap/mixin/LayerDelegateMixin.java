package com.toroidalworld.compat.journeymap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.toroidalworld.compat.journeymap.JourneyMapFold;

import net.minecraft.core.BlockPos;

@Mixin(targets = "journeymap.client.ui.fullscreen.layer.LayerDelegate", remap = false)
public class LayerDelegateMixin {
    @ModifyReturnValue(method = "getBlockPos", at = @At("RETURN"))
    private BlockPos toroidal$foldMouseBlock(BlockPos original) {
        return JourneyMapFold.foldUiBlock(original);
    }
}
