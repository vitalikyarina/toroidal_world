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
import net.minecraft.server.level.LoadingChunkTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.SimulationChunkTracker;

// The distance manager owns every ticket graph, so binding the level here reaches all of them at once. The cross-seam
// neighbour relation itself lives in the graphs now — every tracker folds its neighbour walk (ChunkTrackerMixin) — so
// the companion-ticket machinery that used to be issued from this pass is gone.
@Mixin(DistanceManager.class)
public class DistanceManagerMixin implements LevelBindable {
    @Shadow
    @Final
    private LoadingChunkTracker loadingChunkTracker;

    @Shadow
    @Final
    private SimulationChunkTracker simulationChunkTracker;

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
        ((LevelBindable) this.loadingChunkTracker).toroidal$bindLevel(level);
        ((LevelBindable) this.simulationChunkTracker).toroidal$bindLevel(level);
        ((LevelBindable) this.naturalSpawnChunkCounter).toroidal$bindLevel(level);
        ((LevelBindable) this.playerTicketManager).toroidal$bindLevel(level);
    }

    // Whether an entity is ticked at all is decided by this gate, asked of the raw chunk the entity's coordinate names.
    // An entity pushed a step past the bounds still stands in a real chunk — the wrapped one — but the raw chunk is one
    // the manager never heard of, so the gate says no, the entity is skipped, and the tick-tail wrap that would bring
    // it home never runs. It is the same question the tracker already asks correctly for isChunkTracked.
    @ModifyVariable(method = "inEntityTickingRange", at = @At("HEAD"), argsOnly = true)
    private long toroidal$entityTickingOnPhysicalChunk(long chunkKey) {
        if (this.toroidal$level == null) {
            return chunkKey;
        }

        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(this.toroidal$level);
        return transformer == null ? chunkKey : transformer.chunks.wrapChunkKey(chunkKey);
    }
}
