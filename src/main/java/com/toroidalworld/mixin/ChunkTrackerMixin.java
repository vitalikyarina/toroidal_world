package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.LevelBindable;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.server.level.ChunkTracker;
import net.minecraft.server.level.ServerLevel;

// The ticket graphs decide which chunks are loaded, by spreading a distance level through neighbouring chunks. Their
// neighbours are computed in unbounded space, so the chunks on the far side of the seam are a whole world away and
// never get a level — they stay unloaded, and a player looking across the seam sees the void.
//
// An out-of-bounds neighbour is REPLACED by its wrapped (physical) chunk, not added beside it: one physical chunk must
// hold exactly one live key, or the same chunk carries levels and tickets under both representations and every seam
// crossing churns as the duplicate band swaps sides. EVERY graph folds, the LoadingChunkTracker included:
// the loading graph's folded edge gives the chunk across the seam level+1 per hop — exactly what the companion-ticket
// machinery used to hand-simulate, but distance-correct and settled in the same runAllUpdates pass. With it folded, no
// out-of-bounds key is ever enumerated, so updateChunkScheduling never creates a phantom holder; the generation cache
// folds its slots to the physical chunks the graph now levels (ChunkGenerationTaskMixin).
@Mixin(ChunkTracker.class)
public class ChunkTrackerMixin implements LevelBindable {
    @Unique
    private @Nullable ServerLevel toroidal$level;

    @Unique
    private @Nullable WorldLoopTransformer toroidal$boundTransformer;

    @Override
    public void toroidal$bindLevel(ServerLevel level) {
        this.toroidal$level = level;
    }

    // Both neighbour walks — the spread (checkNeighborsAfterUpdate) and the recompute (getComputedLevel) — enumerate
    // the eight raw ±1 keys around the node at this one call. Substituting the physical key here is the whole seam fix:
    // vanilla's own guards then do the rest against the only key that is live — the self cell folds to the source
    // sentinel, the known parent is skipped, and the level is read from where it is actually stored.
    @WrapOperation(
            method = {"checkNeighborsAfterUpdate", "getComputedLevel"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ChunkPos;pack(II)J"),
            expect = 2)
    private long toroidal$physicalNeighborKey(int chunkX, int chunkZ, Operation<Long> original) {
        WorldLoopTransformer transformer = this.toroidal$transformer();
        if (!transformer.isWrapped()) {
            return original.call(chunkX, chunkZ);
        }

        return original.call(transformer.chunks.x.wrap(chunkX), transformer.chunks.z.wrap(chunkZ));
    }

    // Every graph folds. Cycles through the seam cannot self-sustain for the same reason the plane's own cycles
    // cannot: a level is the shortest distance to a source, and L = L + 2 has no solution — remove the sources and
    // the recompute drives every level past the unload threshold, on the torus exactly as on the plane.
    @Unique
    private WorldLoopTransformer toroidal$transformer() {
        return this.toroidal$resolvedTransformer();
    }

    // Resolved on first use and kept: a tracker belongs to one level for its whole life, and a level's transformer is
    // decided once by its generator. An unbound tracker answers NOOP without storing it — memoizing that would pin the
    // graph to an unwrapped world for good, which is the same trap as resolving at construction.
    @Unique
    private WorldLoopTransformer toroidal$resolvedTransformer() {
        WorldLoopTransformer transformer = this.toroidal$boundTransformer;
        if (transformer != null) {
            return transformer;
        }

        if (this.toroidal$level == null) {
            return WorldLoopTransformer.NOOP;
        }

        transformer = WorldLoopAttachments.transformerOf(this.toroidal$level);
        this.toroidal$boundTransformer = transformer;
        return transformer;
    }
}
