package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.accessors.LevelBindable;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TickingTracker;

// The distance manager owns every ticket graph, so binding the level here reaches all of them at once. The cross-seam
// neighbour relation itself lives in the graphs now — every tracker folds its neighbour walk (ChunkTrackerMixin) — so
// the companion-ticket machinery that used to be issued from this pass is gone.
//
// It also owns the ticket map itself: every ticket is a source of the loading graph, and with the graph folded at the
// seam no source may name ground past the bounds — a raw out-of-bounds key would raise the one holder the whole
// removal exists to make impossible. All ticket traffic funnels through the two long-keyed primitives (the ChunkPos
// and radius overloads and /forceload all call down into them), so the key is folded here once rather than at every
// caller. The second graph those same paths feed is folded at its own primitive (TickingTrackerMixin).
@Mixin(DistanceManager.class)
public class DistanceManagerMixin implements LevelBindable {
    @Shadow
    @Final
    private DistanceManager.ChunkTicketTracker ticketTracker;

    @Shadow
    @Final
    private TickingTracker tickingTicketsTracker;

    @Shadow
    @Final
    private DistanceManager.FixedPlayerDistanceChunkTracker naturalSpawnChunkCounter;

    @Shadow
    @Final
    private DistanceManager.PlayerTicketTracker playerTicketManager;

    @Unique
    private @Nullable ServerLevel toroidal$level;

    @Override
    public void toroidal$bindLevel(ServerLevel level) {
        this.toroidal$level = level;
        ((LevelBindable) this.ticketTracker).toroidal$bindLevel(level);
        ((LevelBindable) this.tickingTicketsTracker).toroidal$bindLevel(level);
        ((LevelBindable) this.naturalSpawnChunkCounter).toroidal$bindLevel(level);
        ((LevelBindable) this.playerTicketManager).toroidal$bindLevel(level);
    }

    @ModifyVariable(method = "addTicket(JLnet/minecraft/server/level/Ticket;)V", at = @At("HEAD"), argsOnly = true)
    private long toroidal$foldAddedTicketKey(long key) {
        return this.toroidal$foldKey(key);
    }

    @ModifyVariable(method = "removeTicket(JLnet/minecraft/server/level/Ticket;)V", at = @At("HEAD"), argsOnly = true)
    private long toroidal$foldRemovedTicketKey(long key) {
        return this.toroidal$foldKey(key);
    }

    // Whether an entity is ticked at all is decided by this gate, asked of the raw chunk the entity's coordinate names.
    // An entity pushed a step past the bounds still stands in a real chunk — the wrapped one — but the raw chunk is one
    // the manager never heard of, so the gate says no, the entity is skipped, and the tick-tail wrap that would bring
    // it home never runs. It is the same question the tracker already asks correctly for isChunkTracked.
    @ModifyVariable(method = "inEntityTickingRange", at = @At("HEAD"), argsOnly = true)
    private long toroidal$entityTickingOnPhysicalChunk(long chunkKey) {
        return this.toroidal$foldKey(chunkKey);
    }

    @Unique
    private long toroidal$foldKey(long key) {
        WorldLoopTransformer transformer = this.toroidal$wrappedTransformer();
        return transformer == null ? key : transformer.chunks.wrapChunkKey(key);
    }

    @Unique
    private @Nullable WorldLoopTransformer toroidal$wrappedTransformer() {
        return this.toroidal$level == null ? null : WorldLoopAttachments.wrappedTransformerOf(this.toroidal$level);
    }
}
