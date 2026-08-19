package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.player.WaypointLapGate;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.waypoints.WaypointTransmitter;

// Created on first use: a mixin's @Unique field initialisers never run.
@Mixin(WaypointTransmitter.EntityBlockConnection.class)
public class WaypointBlockConnectionMixin {
    @Shadow
    @Final
    private ServerPlayer receiver;

    @Unique
    private @Nullable WaypointLapGate toroidal$lapGate;

    @WrapOperation(
            method = "update",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;distManhattan(Lnet/minecraft/core/Vec3i;)I"))
    private int toroidal$resendWhenTheReceiverLaps(BlockPos currentPosition, Vec3i lastPosition, Operation<Integer> original) {
        if (this.toroidal$lapGate == null) {
            this.toroidal$lapGate = new WaypointLapGate();
        }

        return this.toroidal$lapGate.widen(original.call(currentPosition, lastPosition), this.receiver, currentPosition);
    }
}
