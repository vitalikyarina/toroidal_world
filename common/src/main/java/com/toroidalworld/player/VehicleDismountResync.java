package com.toroidalworld.player;

import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class VehicleDismountResync {
    public static void resyncAfterDismount(Entity passenger) {
        if (!(passenger instanceof ServerPlayer player)) {
            return;
        }

        Entity vehicle = player.getVehicle();
        if (vehicle == null || WorldLoopAttachments.wrappedTransformerOf(player.level()) == null) {
            return;
        }

        MinecraftServer server = player.level().getServer();
        server.schedule(new TickTask(server.getTickCount(), () -> {
            if (vehicle.isAlive() && player.getControlledVehicle() != vehicle) {
                player.connection.send(ClientboundEntityPositionSyncPacket.of(vehicle));
            }
        }));
    }

    private VehicleDismountResync() {
    }
}
