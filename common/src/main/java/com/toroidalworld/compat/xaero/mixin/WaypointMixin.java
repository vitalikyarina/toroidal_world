package com.toroidalworld.compat.xaero.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.toroidalworld.compat.xaero.XaeroFold;

import net.minecraft.core.Direction;

@Mixin(targets = "xaero.common.minimap.waypoints.Waypoint", remap = false)
public abstract class WaypointMixin {
    @ModifyReturnValue(method = "getX(D)I", at = @At("RETURN"))
    private int toroidal$foldX(int original) {
        return XaeroFold.nearestWaypointBlock(Direction.Axis.X, original);
    }

    @ModifyReturnValue(method = "getZ(D)I", at = @At("RETURN"))
    private int toroidal$foldZ(int original) {
        return XaeroFold.nearestWaypointBlock(Direction.Axis.Z, original);
    }
}
