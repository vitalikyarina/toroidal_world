package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.engine.seam.WaypointLapGate;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.waypoints.WaypointTransmitter;

@Mixin(WaypointTransmitter.EntityChunkConnection.class)
public class WaypointChunkConnectionMixin {
    @Shadow
    @Final
    private ServerPlayer receiver;

    @Unique
    private final WaypointLapGate toroidal$lapGate = new WaypointLapGate();

    @WrapOperation(
            method = "update",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/ChunkPos;getChessboardDistance(Lnet/minecraft/world/level/ChunkPos;)I"))
    private int toroidal$resendWhenTheReceiverLaps(ChunkPos currentPosition, ChunkPos lastPosition, Operation<Integer> original) {
        return this.toroidal$lapGate.widen(original.call(currentPosition, lastPosition), this.receiver, currentPosition);
    }
}
