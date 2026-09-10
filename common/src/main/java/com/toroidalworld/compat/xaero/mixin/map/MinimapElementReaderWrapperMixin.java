package com.toroidalworld.compat.xaero.mixin.map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.toroidalworld.compat.xaero.XaeroWorldMapFold;

import net.minecraft.core.Direction;

import xaero.map.mods.minimap.element.MinimapElementReaderWrapper;

@Mixin(value = MinimapElementReaderWrapper.class, remap = false)
public abstract class MinimapElementReaderWrapperMixin {
    @ModifyReturnValue(method = "getRenderX", at = @At("RETURN"))
    private double toroidal$foldRenderX(double original) {
        return XaeroWorldMapFold.foldCoord(Direction.Axis.X, original);
    }

    @ModifyReturnValue(method = "getRenderZ", at = @At("RETURN"))
    private double toroidal$foldRenderZ(double original) {
        return XaeroWorldMapFold.foldCoord(Direction.Axis.Z, original);
    }
}
