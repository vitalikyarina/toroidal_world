package com.toroidalworld.compat.xaero.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.toroidalworld.compat.xaero.XaeroFold;

import net.minecraft.core.BlockPos;

@Mixin(targets = "xaero.hud.minimap.world.state.MinimapWorldStateUpdater", remap = false)
public abstract class MinimapWorldStateUpdaterMixin {
    @ModifyReturnValue(method = "getAutoWorldNodeBase", at = @At("RETURN"))
    private Object toroidal$foldSpawnNodeBase(Object original) {
        return original instanceof BlockPos spawn ? XaeroFold.foldWorldNodeSpawn(spawn) : original;
    }
}
