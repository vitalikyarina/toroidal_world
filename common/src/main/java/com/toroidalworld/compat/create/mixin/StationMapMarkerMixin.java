package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.station.StationMarker;
import com.toroidalworld.compat.create.CreateStationMapFold;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

@Mixin(value = MapItemSavedData.class, priority = 1100)
public class StationMapMarkerMixin {
    @Shadow
    @Final
    public int centerX;

    @Shadow
    @Final
    public int centerZ;

    @Shadow
    @Final
    public ResourceKey<Level> dimension;

    @ModifyVariable(method = "addStationMarker", at = @At("HEAD"), argsOnly = true, remap = false)
    private StationMarker toroidal$canonicalStationMarker(StationMarker marker) {
        BlockPos saved = marker.getTarget();
        BlockPos canonical = CreateStationMapFold.canonicalTarget(this.dimension, saved);
        return canonical.equals(saved) ? marker : new StationMarker(marker.getSource(), canonical, marker.getName());
    }

    @WrapOperation(method = "addStationMarker",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/trains/station/StationMarker;"
                            + "getTarget()Lnet/minecraft/core/BlockPos;",
                    remap = false),
            remap = false)
    private BlockPos toroidal$markerTargetInMapFrame(StationMarker marker, Operation<BlockPos> original) {
        return CreateStationMapFold.targetInMapFrame(this.dimension, this.centerX, this.centerZ,
                original.call(marker));
    }

    @ModifyVariable(method = "toggleStation", at = @At("STORE"), ordinal = 0, remap = false)
    private double toroidal$toggledStationXInMapFrame(double xCenter) {
        return CreateStationMapFold.xInMapFrame(this.dimension, this.centerX, xCenter);
    }

    @ModifyVariable(method = "toggleStation", at = @At("STORE"), ordinal = 1, remap = false)
    private double toroidal$toggledStationZInMapFrame(double zCenter) {
        return CreateStationMapFold.zInMapFrame(this.dimension, this.centerZ, zCenter);
    }
}
