package com.toroidalworld.compat.journeymap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.compat.journeymap.JourneyMapFold;

import net.minecraft.core.Direction;

@Mixin(targets = "journeymap.client.ui.option.LocationFormat$LocationFormatKeys", remap = false)
public class LocationFormatMixin {
    @ModifyVariable(method = "format", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int toroidal$foldX(int x) {
        return JourneyMapFold.foldUiCoord(Direction.Axis.X, x);
    }

    @ModifyVariable(method = "format", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private int toroidal$foldZ(int z) {
        return JourneyMapFold.foldUiCoord(Direction.Axis.Z, z);
    }
}
