package com.toroidalworld.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;

//
// Filtered where the batch is still a list rather than at each acceptor call inside sendPairingData: that method is the
// one frame NeoForge patches, swapping the vanilla Consumer for its own PacketAndPayloadAcceptor, so an anchor there
// has to be written twice and kept in step with a loader. The bundle is built identically on both, and the list is the
// only place the whole batch exists as data.
@Mixin(ServerEntity.class)
public class ServerEntityMixin {
    @Shadow
    @Final
    private Entity entity;

    @Shadow
    @Final
    private ServerLevel level;

    @ModifyArg(
            method = "addPairing",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/game/ClientboundBundlePacket;<init>(Ljava/lang/Iterable;)V"))
    private Iterable<Packet<? super ClientGamePacketListener>> toroidal$dropPrematureVehiclePassengers(
            Iterable<Packet<? super ClientGamePacketListener>> pairing, @Local(argsOnly = true) ServerPlayer player) {
        if (WorldLoopAttachments.wrappedTransformerOf(this.level) == null) {
            return pairing;
        }

        Entity vehicle = this.entity.getVehicle();
        if (vehicle == null || toroidal$isWatching(player, vehicle)) {
            return pairing;
        }

        List<Packet<? super ClientGamePacketListener>> announced = new ArrayList<>();
        for (Packet<? super ClientGamePacketListener> packet : pairing) {
            if (!(packet instanceof ClientboundSetPassengersPacket passengers)
                    || passengers.getVehicle() == this.entity.getId()) {
                announced.add(packet);
            }
        }

        return announced;
    }

    @Unique
    private boolean toroidal$isWatching(ServerPlayer player, Entity watched) {
        ChunkMap.TrackedEntity tracked = this.level.getChunkSource().chunkMap.entityMap.get(watched.getId());
        if (tracked == null) {
            return false;
        }

        for (ServerPlayerConnection connection : tracked.seenBy) {
            if (connection.getPlayer() == player) {
                return true;
            }
        }

        return false;
    }
}
