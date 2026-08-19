package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.LevelBindable;
import com.toroidalworld.accessors.TransformerCache;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.server.level.ChunkTracker;
import net.minecraft.server.level.ServerLevel;

@Mixin(ChunkTracker.class)
public class ChunkTrackerMixin implements LevelBindable, TransformerCache {
    @Unique
    private @Nullable ServerLevel toroidal$level;

    @Unique
    private @Nullable WorldLoopTransformer toroidal$boundTransformer;

    @Override
    public void toroidal$bindLevel(ServerLevel level) {
        this.toroidal$level = level;
    }

    @Override
    public @Nullable ServerLevel toroidal$boundLevel() {
        return this.toroidal$level;
    }

    @WrapOperation(
            method = {"checkNeighborsAfterUpdate", "getComputedLevel"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ChunkPos;asLong(II)J"),
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
    //
    // Resolved on first use and kept: a tracker belongs to one level for its whole life, and a level's transformer is
    // decided once by its generator. An unbound tracker answers NOOP without storing it — memoizing that would pin the
    // graph to an unwrapped world for good, which is the same trap as resolving at construction.
    //
    // Exposed as TransformerCache because the subclasses that own ticket maps of their own — TickingTracker — have to
    // fold their keys with the same transformer, and MUST NOT bind a level of their own to get it: LevelBindable is
    // implemented here, so a subclass implementing it again would override this one, leave toroidal$level unset for
    // that tracker, and silently unfold its neighbour walk.
    @Override
    public WorldLoopTransformer toroidal$transformer() {
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
