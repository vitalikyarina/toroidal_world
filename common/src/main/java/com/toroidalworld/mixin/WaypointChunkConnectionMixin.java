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

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.waypoints.WaypointTransmitter;

// Created on first use: a mixin's @Unique field initialisers never run.
@Mixin(WaypointTransmitter.EntityChunkConnection.class)
public class WaypointChunkConnectionMixin {
    @Shadow
    @Final
    private ServerPlayer receiver;

    @Unique
    private @Nullable WaypointLapGate toroidal$lapGate;

    @WrapOperation(
            method = "update",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/ChunkPos;getChessboardDistance(Lnet/minecraft/world/level/ChunkPos;)I"))
    private int toroidal$resendWhenTheReceiverLaps(ChunkPos currentPosition, ChunkPos lastPosition, Operation<Integer> original) {
        if (this.toroidal$lapGate == null) {
            this.toroidal$lapGate = new WaypointLapGate();
        }

        return this.toroidal$lapGate.widen(original.call(currentPosition, lastPosition), this.receiver, currentPosition);
    }
}
