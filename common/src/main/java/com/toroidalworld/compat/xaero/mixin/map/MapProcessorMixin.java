package com.toroidalworld.compat.xaero.mixin.map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.compat.xaero.XaeroWorldMapFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import xaero.map.MapProcessor;

@Mixin(value = MapProcessor.class, remap = false)
public abstract class MapProcessorMixin {
    @Inject(method = "getAutoIdBase", at = @At("RETURN"), cancellable = true)
    private void toroidal$foldIdSpawn(CallbackInfoReturnable<Object> cir) {
        if (cir.getReturnValue() instanceof BlockPos spawn) {
            BlockPos folded = XaeroWorldMapFold.foldIdSpawn(spawn);
            if (folded != spawn) {
                cir.setReturnValue(folded);
            }
        }
    }

    @ModifyArg(
            method = "updateFootprints",
            at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;add(Ljava/lang/Object;)Z"))
    private Object toroidal$foldFootprint(Object footprint) {
        if (!(footprint instanceof Double[] coords) || coords.length != 2 || !XaeroWorldMapFold.active()) {
            return footprint;
        }

        double foldedX = XaeroWorldMapFold.foldFootprintCoord(Direction.Axis.X, coords[0]);
        double foldedZ = XaeroWorldMapFold.foldFootprintCoord(Direction.Axis.Z, coords[1]);
        return foldedX == coords[0] && foldedZ == coords[1] ? footprint : new Double[] {foldedX, foldedZ};
    }
}
