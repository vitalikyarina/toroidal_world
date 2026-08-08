package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.accessors.LevelBindable;
import com.toroidalworld.accessors.LevelBindRegistry;

import net.minecraft.server.level.DistanceManager;
import net.minecraft.world.level.TicketStorage;

// The loading graph must fold like every other ChunkTracker, but its class is package-private, so DistanceManagerMixin
// cannot shadow the field to bind it. The tracker instead registers itself with the TicketStorage it listens to, and
// TicketStorageMixin forwards its own level bind to it.
@Mixin(targets = "net.minecraft.server.level.LoadingChunkTracker")
public class LoadingChunkTrackerMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void toroidal$registerForBind(DistanceManager distanceManager, TicketStorage ticketStorage,
            CallbackInfo ci) {
        ((LevelBindRegistry) ticketStorage).toroidal$registerBindable((LevelBindable) (Object) this);
    }
}
