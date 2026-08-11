package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.accessors.TransformerCache;
import com.toroidalworld.core.WorldLoopTransformer;

import net.minecraft.server.level.TickingTracker;

// The second place a raw ticket key enters. The region and forced paths hand the same key to two graphs in two
// separate calls — this.addTicket(key, ticket) and this.tickingTicketsTracker.addTicket(key, ticket) — so folding in
// the distance manager alone would leave this graph keyed past the bounds, and nothing would say so until something
// asked it a question. Folding its own two long-keyed primitives is the same statement made where this graph keeps
// its own ticket map: the map and the spread must agree on the key, or getLevelFromSource reads empty at the folded
// one. Add and remove fold identically, or a ticket added folded could never be found again to remove.
//
// The transformer comes from ChunkTrackerMixin, which this class extends, and is deliberately not bound again here —
// see the note on ChunkTrackerMixin.
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
