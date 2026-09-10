package com.toroidalworld.compat.xaero.mixin.map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.toroidalworld.compat.xaero.XaeroWorldMapFold;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import xaero.map.MapProcessor;

@Mixin(value = MapProcessor.class, remap = false)
public abstract class MapProcessorMixin {
    @Shadow
    public ClientLevel mainWorld;

    @ModifyReturnValue(method = "getAutoIdBase", at = @At("RETURN"))
    private Object toroidal$foldIdSpawn(Object original, ClientLevel world) {
        return original instanceof BlockPos spawn ? XaeroWorldMapFold.foldIdSpawn(world, spawn) : original;
    }

    @ModifyArg(
            method = "updateFootprints",
            at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;add(Ljava/lang/Object;)Z"))
    private Object toroidal$foldFootprint(Object footprint) {
        if (!(footprint instanceof Double[] coords) || coords.length != 2) {
            return footprint;
        }

        double foldedX = XaeroWorldMapFold.foldFootprintCoord(this.mainWorld, Direction.Axis.X, coords[0]);
        double foldedZ = XaeroWorldMapFold.foldFootprintCoord(this.mainWorld, Direction.Axis.Z, coords[1]);
        return foldedX == coords[0] && foldedZ == coords[1] ? footprint : new Double[] {foldedX, foldedZ};
    }
}
