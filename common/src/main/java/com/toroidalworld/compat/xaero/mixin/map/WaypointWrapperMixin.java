package com.toroidalworld.compat.xaero.mixin.map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.compat.xaero.XaeroWorldMapFold;

import net.minecraft.core.Direction;

@Mixin(value = xaero.map.mods.gui.Waypoint.class, remap = false)
public abstract class WaypointWrapperMixin {
    @Shadow
    private double dimDiv;

    @Inject(method = "getX", at = @At("RETURN"), cancellable = true)
    private void toroidal$foldX(CallbackInfoReturnable<Integer> cir) {
        if (this.dimDiv == 1.0) {
            int folded = XaeroWorldMapFold.foldWaypointBlock(Direction.Axis.X, cir.getReturnValue());
            if (folded != cir.getReturnValue()) {
                cir.setReturnValue(folded);
            }
        }
    }

    @Inject(method = "getZ", at = @At("RETURN"), cancellable = true)
    private void toroidal$foldZ(CallbackInfoReturnable<Integer> cir) {
        if (this.dimDiv == 1.0) {
            int folded = XaeroWorldMapFold.foldWaypointBlock(Direction.Axis.Z, cir.getReturnValue());
            if (folded != cir.getReturnValue()) {
                cir.setReturnValue(folded);
            }
        }
    }
}
