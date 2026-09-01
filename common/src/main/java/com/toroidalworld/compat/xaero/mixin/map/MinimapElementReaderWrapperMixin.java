package com.toroidalworld.compat.xaero.mixin.map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.compat.xaero.XaeroWorldMapFold;

import net.minecraft.core.Direction;

import xaero.map.mods.minimap.element.MinimapElementReaderWrapper;

@Mixin(value = MinimapElementReaderWrapper.class, remap = false)
public abstract class MinimapElementReaderWrapperMixin {
    @Inject(method = "getRenderX", at = @At("RETURN"), cancellable = true)
    private void toroidal$foldRenderX(CallbackInfoReturnable<Double> cir) {
        double folded = XaeroWorldMapFold.foldElementCoord(Direction.Axis.X, cir.getReturnValue());
        if (folded != cir.getReturnValue()) {
            cir.setReturnValue(folded);
        }
    }

    @Inject(method = "getRenderZ", at = @At("RETURN"), cancellable = true)
    private void toroidal$foldRenderZ(CallbackInfoReturnable<Double> cir) {
        double folded = XaeroWorldMapFold.foldElementCoord(Direction.Axis.Z, cir.getReturnValue());
        if (folded != cir.getReturnValue()) {
            cir.setReturnValue(folded);
        }
    }
}
