package com.toroidalworld.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

// Pairing a passenger also announces its vehicle's passenger list, but pairing order across entities is arbitrary: a
// passenger paired before its vehicle names an id the client does not hold yet ("Received passengers for unknown
// entity"), and the constant drop-and-re-pair a jockey goes through at the seam turns that rare vanilla race chronic.
// The premature packet is dropped — the vehicle's own pairing sends the authoritative list in the same tick.
@Mixin(ServerEntity.class)
public class ServerEntityMixin {
    @Shadow
    @Final
    private Entity entity;

    @Shadow
    @Final
    private ServerLevel level;

    @WrapWithCondition(
            method = "sendPairingData",
            at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"))
    private boolean toroidal$suppressPrematureVehiclePassengers(Consumer<?> broadcast, Object packet,
            @Local(argsOnly = true) ServerPlayer player) {
        if (!(packet instanceof ClientboundSetPassengersPacket passengers)
                || passengers.getVehicle() == this.entity.getId()) {
            return true;
        }

        if (WorldLoopAttachments.wrappedTransformerOf(this.level) == null) {
            return true;
        }

        Entity vehicle = this.entity.getVehicle();
        return vehicle == null || this.level.getChunkSource().chunkMap.getPlayersWatching(vehicle).contains(player);
    }
}
