package com.toroidalworld.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.toroidalworld.accessors.TransformerHolder;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;

// Concentric rings (strongholds) are the one placement that radiates from the origin in raw coordinates instead of
// walking a grid, and the ring positions never meet the bounds: the first ring sits at 4 x distance chunks — with
// vanilla's distance of 32 that is ~128 chunks out, past the edge of any real looped world. A position past the bounds
// names a phantom chunk; the real chunk occupying that spot asks isStructureChunk with its canonical coordinates, the
// list holds raw ones, and the stronghold is placed nowhere — the world has no way to reach the End.
//
// The list is corrected once, where it is generated, and every reader — placement, locate, eye of ender — sees the same
// truth. On a world wide enough the correction changes nothing: in-bounds positions pass through untouched, so
// generation stays byte-for-byte vanilla, and only the dead out-of-bounds entries are dropped (they never generated
// anything, but the concentric locate loop would probe them and pull real generation out of phantom chunks). Only when
// nothing survives — the world is narrower than the first ring — the first ring position is folded in: exactly one
// stronghold, because the guarantee is End access, not vanilla's 128-count density packed into a small torus. Rings
// grow outward monotonically, so "first ring out but an outer position in" cannot occur.
//
// The state is built by the generator but never keeps a reference to it, so the generator stamps its transformer here
// at createState; a state without the stamp keeps the NOOP transformer and vanilla behaviour.
@Mixin(ChunkGeneratorStructureState.class)
public abstract class ChunkGeneratorStructureStateMixin implements TransformerHolder {
    // How far the biome search can move a ring position from the centre it was handed. It searches a radius of 112
    // blocks, so the block it answers with lies in [centre*16 - 104, centre*16 + 120], which floor-divides to exactly
    // centre +/- 7 chunks. A centre further out of bounds than this cannot be pulled back in by any answer.
    @Unique
    private static final int toroidal$BIOME_SEARCH_REACH_CHUNKS = 7;

    @Unique
    private WorldLoopTransformer toroidal$transformer = WorldLoopTransformer.NOOP;

    @Override
    public WorldLoopTransformer toroidal$transformer() {
        return this.toroidal$transformer;
    }

    @Override
    public void toroidal$setTransformer(WorldLoopTransformer transformer) {
        this.toroidal$transformer = transformer;
    }

    // Each ring position fans out its own task onto the shared background pool, and the biome search inside it is the
    // single largest noise consumer of a world load — 128 searches of a 224x224 block square, ~86 million ImprovedNoise
    // samples. Nothing bound the transformer there, so every one of them answered from non-periodic noise: the search
    // exists to bias a stronghold onto land, and it was reading land that this world does not have.
    //
    // The supplier is wrapped rather than the search itself because the search sits in a lambda — a synthetic method
    // whose name depends on compilation order — while supplyAsync is a unique call in the method's own body. Wrapping
    // it binds inside the task, on the thread that runs it, which is the only place a thread-local binding survives to.
    //
    // Bound unconditionally, NOOP included, for the reason NoiseBasedChunkGeneratorMixin binds unconditionally: this is
    // a shared pool, and a scoped bind that overwrites and restores leaves the thread as clean as it found it.
    //
    // A position the bounds filter below will drop whatever the search answers costs nothing to leave unsearched: the
    // task returns vanilla's own no-biome-found value, the raw ring position, and the filter drops that instead. On a
    // world of 256 chunks every ring past the first sits at 280 chunks or further, so this is most of the 128.
    //
    // The first position is never skipped. It seeds the last resort of theWorldsShare — on a world narrower than the
    // first ring nothing survives the filter, and that one wrapped position is the world's only stronghold. Skipping it
    // would hand the fold a raw ring point instead of a searched one, which is exactly the land bias this search exists
    // for. The locals are captured by name rather than ordinal: eight ints are in scope here, and a shifted ordinal
    // would read the wrong one silently, where a missing name fails at apply time.
    @ModifyArg(
            method = "generateRingPositions",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/concurrent/CompletableFuture;supplyAsync(Ljava/util/function/Supplier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"),
            index = 0)
    private Supplier<ChunkPos> toroidal$searchOnThisWorldsNoise(Supplier<ChunkPos> search,
            @Local(name = "i") int ringIndex,
            @Local(name = "initialX") int ringChunkX,
            @Local(name = "initialZ") int ringChunkZ) {
        WorldLoopTransformer transformer = this.toroidal$transformer;
        if (ringIndex > 0
                && transformer.chunks.overshoot(ringChunkX, ringChunkZ) > toroidal$BIOME_SEARCH_REACH_CHUNKS) {
            ChunkPos beyondTheWorld = new ChunkPos(ringChunkX, ringChunkZ);
            return () -> beyondTheWorld;
        }

        return () -> GenerationTransformerContext.withTransformer(transformer, search);
    }

    @ModifyReturnValue(method = "generateRingPositions", at = @At("RETURN"))
    private CompletableFuture<List<ChunkPos>> toroidal$ringsWithinTheWorld(
            CompletableFuture<List<ChunkPos>> original) {
        WorldLoopTransformer transformer = this.toroidal$transformer;
        if (!transformer.isWrapped()) {
            return original;
        }

        return original.thenApply(positions -> toroidal$theWorldsShare(transformer, positions));
    }

    // The folded position sits in the same biome as its raw original — the search above runs on the bound, periodic
    // chain — so the ring's own biome search loses nothing by running in raw coordinates first.
    @Unique
    private static List<ChunkPos> toroidal$theWorldsShare(WorldLoopTransformer transformer, List<ChunkPos> positions) {
        List<ChunkPos> inBounds = new ArrayList<>(positions.size());
        for (ChunkPos position : positions) {
            if (!transformer.chunks.isOver(position)) {
                inBounds.add(position);
            }
        }

        if (inBounds.size() == positions.size()) {
            return positions;
        }

        if (!inBounds.isEmpty()) {
            return List.copyOf(inBounds);
        }

        return List.of(transformer.chunks.wrap(positions.getFirst()));
    }
}
