package com.toroidalworld.compat.xaero.mixin.map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.toroidalworld.compat.xaero.XaeroWorldMapFold;

import net.minecraft.core.Direction;

@Mixin(value = xaero.map.mods.gui.Waypoint.class, remap = false)
public abstract class WaypointWrapperMixin {
    @Shadow
    private double dimDiv;

    @ModifyReturnValue(method = "getX", at = @At("RETURN"))
    private int toroidal$foldX(int original) {
        return this.dimDiv == 1.0 ? XaeroWorldMapFold.foldBlock(Direction.Axis.X, original) : original;
    }

    @ModifyReturnValue(method = "getZ", at = @At("RETURN"))
    private int toroidal$foldZ(int original) {
        return this.dimDiv == 1.0 ? XaeroWorldMapFold.foldBlock(Direction.Axis.Z, original) : original;
    }
}
