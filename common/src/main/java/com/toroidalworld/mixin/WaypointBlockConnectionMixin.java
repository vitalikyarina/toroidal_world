package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.engine.seam.WaypointLapGate;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.waypoints.WaypointTransmitter;

@Mixin(WaypointTransmitter.EntityBlockConnection.class)
public class WaypointBlockConnectionMixin {
    @Shadow
    @Final
    private ServerPlayer receiver;

    @Unique
    private final WaypointLapGate toroidal$lapGate = new WaypointLapGate();

    @WrapOperation(
            method = "update",
            at = @At(value = "INVOKE",
                    target = InjectionTargets.BLOCK_POS_DIST_MANHATTAN))
    private int toroidal$resendWhenTheReceiverLaps(BlockPos currentPosition, Vec3i lastPosition, Operation<Integer> original) {
        return this.toroidal$lapGate.widen(original.call(currentPosition, lastPosition), this.receiver, currentPosition);
    }
}
