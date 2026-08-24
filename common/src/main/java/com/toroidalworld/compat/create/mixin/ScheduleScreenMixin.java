package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.schedule.ScheduleScreen;
import com.toroidalworld.compat.create.client.CreateClientFrame;

import net.minecraft.world.phys.Vec3;

@Mixin(value = ScheduleScreen.class, remap = false)
public abstract class ScheduleScreenMixin {
    @WrapOperation(
            method = "*",
            require = 1,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;distanceTo(Lnet/minecraft/world/phys/Vec3;)D"),
            allow = 1)
    private static double toroidal$stationDistanceInTheViewerFrame(Vec3 station, Vec3 viewer,
            Operation<Double> original) {
        return original.call(CreateClientFrame.nearestCopy(viewer, station), viewer);
    }
}
