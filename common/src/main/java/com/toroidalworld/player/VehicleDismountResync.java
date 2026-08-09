package com.toroidalworld.player;

import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

// The rider predicts their own vehicle, so the translator drops the tracker's position syncs for it — but the tracker
// records every dropped sync as delivered, and once the dismounted vehicle stands still it never sends another: the
// client is left holding its last self-predicted pose (a horse frozen mid-jump, a block up in the air). The debt is
// repaid at the one moment the mute ends — dismount — with a single absolute sync of the former vehicle.
//
// Called (from EntityMixin) at the head of removeVehicle, before the detach, when the vehicle is still "own" and the
// sync would be dropped again, so the send is deferred — via tell(TickTask), never execute(): on the server thread
// execute() runs the task inline in the same call stack, still before the detach. The queued task runs after it; a
// cancelled dismount is caught by the controlled-vehicle check.
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
                player.connection.send(new ClientboundTeleportEntityPacket(vehicle));
            }
        }));
    }

    private VehicleDismountResync() {
    }
}
