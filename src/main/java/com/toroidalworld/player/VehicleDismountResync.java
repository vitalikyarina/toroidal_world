package com.toroidalworld.player;

import com.toroidalworld.storage.WorldLoopAttachments;
import com.toroidalworld.ToroidalWorld;

import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;

// The rider predicts their own vehicle, so the translator drops the tracker's position syncs for it — but the tracker
// records every dropped sync as delivered, and once the dismounted vehicle stands still it never sends another: the
// client is left holding its last self-predicted pose (a horse frozen mid-jump, a block up in the air). The debt is
// repaid at the one moment the mute ends — dismount — with a single absolute sync of the former vehicle.
//
// The event fires before the detach, when the vehicle is still "own" and the sync would be dropped again, so the send
// is deferred — via tell(TickTask), never execute(): on the server thread execute() runs the task inline in the same
// call stack, still before the detach. The queued task runs after it; a cancelled dismount is caught by the
// controlled-vehicle check.
@EventBusSubscriber(modid = ToroidalWorld.MODID)
public final class VehicleDismountResync {
    @SubscribeEvent
    static void onDismount(EntityMountEvent event) {
        if (!event.isDismounting() || !(event.getEntityMounting() instanceof ServerPlayer player)) {
            return;
        }

        Entity vehicle = event.getEntityBeingMounted();
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
