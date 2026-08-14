package com.toroidalworld.compat.xaero.mixin.map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.compat.xaero.XaeroWorldMapFold;

import net.minecraft.core.Direction;

// The full map's waypoint wrapper delegates to the minimap waypoint's raw stored coordinates — the ones the
// minimap side deliberately leaves unfolded for persistence. Everything the FULL MAP derives from the wrapper
// reads through these two getters: the render position (getRenderX/Z divide these), the right-click tooltip
// string, the sort order. Folded canonical here, all of it lands on the canonical map; the stored minimap
// waypoint stays untouched. Foreign-dimension waypoints (dimDiv != 1) pass through — their coordinates live in
// another dimension's space, which this level's shape does not describe.
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
