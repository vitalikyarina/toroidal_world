package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.accessors.LevelBindable;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;

// Every ticket is a source of the loading graph, and with the graph folded at the seam no source may name ground past
// the bounds — a raw out-of-bounds key would raise the one holder the whole removal exists to make impossible. All
// ticket traffic funnels through the two long-keyed primitives (the ChunkPos and radius overloads, /forceload, and the
// reactivation of deserialized tickets all call down into them), so the key is folded here once rather than at every
// caller. Add and remove fold identically, or a ticket added folded could never be found again to remove.
//
// Bound from ChunkMapMixin's constructor tail, the first moment the level exists.
@Mixin(TicketStorage.class)
public class TicketStorageMixin implements LevelBindable {
    @Unique
    private @Nullable ServerLevel toroidal$level;

    @Override
    public void toroidal$bindLevel(ServerLevel level) {
        this.toroidal$level = level;
    }

    @ModifyVariable(method = "addTicket(JLnet/minecraft/server/level/Ticket;)Z", at = @At("HEAD"), argsOnly = true)
    private long toroidal$foldAddedTicketKey(long key) {
        return this.toroidal$foldKey(key);
    }

    @ModifyVariable(method = "removeTicket(JLnet/minecraft/server/level/Ticket;)Z", at = @At("HEAD"), argsOnly = true)
    private long toroidal$foldRemovedTicketKey(long key) {
        return this.toroidal$foldKey(key);
    }

    @Unique
    private long toroidal$foldKey(long key) {
        if (this.toroidal$level == null) {
            return key;
        }

        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(this.toroidal$level);
        if (transformer == null) {
            return key;
        }

        int chunkX = ChunkPos.getX(key);
        int chunkZ = ChunkPos.getZ(key);
        int wrappedX = transformer.chunks.x.wrap(chunkX);
        int wrappedZ = transformer.chunks.z.wrap(chunkZ);
        return wrappedX == chunkX && wrappedZ == chunkZ ? key : ChunkPos.pack(wrappedX, wrappedZ);
    }
}
