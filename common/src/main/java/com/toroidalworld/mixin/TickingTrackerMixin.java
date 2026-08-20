package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.accessors.TransformerCache;
import com.toroidalworld.core.WorldLoopTransformer;

import net.minecraft.server.level.TickingTracker;

@Mixin(TickingTracker.class)
public class TickingTrackerMixin {
    @ModifyVariable(method = "addTicket(JLnet/minecraft/server/level/Ticket;)V", at = @At("HEAD"), argsOnly = true)
    private long toroidal$foldAddedTicketKey(long key) {
        return this.toroidal$foldTicketKey(key);
    }

    @ModifyVariable(method = "removeTicket(JLnet/minecraft/server/level/Ticket;)V", at = @At("HEAD"), argsOnly = true)
    private long toroidal$foldRemovedTicketKey(long key) {
        return this.toroidal$foldTicketKey(key);
    }

    @Unique
    private long toroidal$foldTicketKey(long key) {
        WorldLoopTransformer transformer = ((TransformerCache) (Object) this).toroidal$transformer();
        return transformer.isWrapped() ? transformer.chunks.wrapChunkKey(key) : key;
    }
}
