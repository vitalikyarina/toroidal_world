package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.accessors.LevelBindable;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.gen.TicketProbe;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TickingTracker;
import net.minecraft.world.level.ChunkPos;

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

    @Shadow
    protected @Nullable ChunkHolder getChunk(long chunkKey) {
        throw new AssertionError();
    }

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
        return this.toroidal$foldTicketKey(key, TicketProbe.OP_ADD);
    }

    @ModifyVariable(method = "removeTicket(JLnet/minecraft/server/level/Ticket;)V", at = @At("HEAD"), argsOnly = true)
    private long toroidal$foldRemovedTicketKey(long key) {
        return this.toroidal$foldTicketKey(key, TicketProbe.OP_REMOVE);
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
    private long toroidal$foldTicketKey(long key, String operation) {
        WorldLoopTransformer transformer = this.toroidal$wrappedTransformer();
        if (transformer == null) {
            return key;
        }

        long folded = transformer.chunks.wrapChunkKey(key);
        TicketProbe.folded(
                this.toroidal$level, transformer, TicketProbe.GRAPH_DISTANCE_MANAGER, operation, key, folded);
        return folded;
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

    // Probe. runAllUpdates is where both graphs have just settled, so the levels read here are the ones the tick ends
    // with. It also runs more than once per tick, which is why the dump gates on the level's own clock.
    @Inject(method = "runAllUpdates", at = @At("HEAD"))
    private void toroidal$dumpTicketCensus(ChunkMap chunkMap, CallbackInfoReturnable<Boolean> cir) {
        WorldLoopTransformer transformer = this.toroidal$wrappedTransformer();
        if (transformer == null || !TicketProbe.shouldDump(this.toroidal$level)) {
            return;
        }

        // The bounds are half-open, so the last chunk the world actually holds is one short of the upper bound; the
        // lower bound is included and names a real chunk as it stands. Sampling the bound itself asks for a chunk the
        // mod's own fold guarantees has no holder, and the sentinel that comes back reads as an abandoned seam.
        int maxChunkX = transformer.chunks.x.upperBound - 1;
        int minChunkX = transformer.chunks.x.lowerBound;
        for (ServerPlayer player : this.toroidal$level.players()) {
            ChunkPos playerChunk = player.chunkPosition();
            int chunkZ = playerChunk.z;
            TicketProbe.seamLevels(
                    this.toroidal$level, player.getName().getString(), playerChunk.x, chunkZ, maxChunkX, minChunkX,
                    this.toroidal$ticketLevel(maxChunkX, chunkZ),
                    this.toroidal$ticketLevel(minChunkX, chunkZ),
                    this.tickingTicketsTracker.getLevel(new ChunkPos(maxChunkX, chunkZ)),
                    this.tickingTicketsTracker.getLevel(new ChunkPos(minChunkX, chunkZ)));
        }
    }

    @Unique
    private int toroidal$ticketLevel(int chunkX, int chunkZ) {
        ChunkHolder holder = this.getChunk(ChunkPos.asLong(chunkX, chunkZ));
        return holder == null ? Integer.MAX_VALUE : holder.getTicketLevel();
    }
}
