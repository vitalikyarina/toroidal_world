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
    // Vanilla searches a radius of 112 blocks, so the answer lies in [centre*16 - 104, centre*16 + 120].
    @Unique
    private static final int toroidal$BIOME_SEARCH_REACH_CHUNKS = 7;

    // The three locals the ring search reads, named by the slot they occupy in generateRingPositions rather than by
    // their source name. A local's name is a property of the decompiler, not of the bytecode that gets loaded: the jar
    // this compiles against calls them j1/k1/l1, and both shipping jars — NeoForge's srg and Fabric's intermediary —
    // call them $$12/$$14/$$15, so a name lookup resolves in dev and fails at apply time in a real game. Slots survive
    // that, and unlike an ordinal they cannot quietly slide onto the neighbouring int when one more comes into scope:
    // eight are live at this instruction, and reading the wrong one is silent.
    @Unique
    private static final int toroidal$RING_POSITION_SLOT = 14;

    @Unique
    private static final int toroidal$RING_CHUNK_X_SLOT = 17;

    @Unique
    private static final int toroidal$RING_CHUNK_Z_SLOT = 18;

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

    @ModifyArg(
            method = "generateRingPositions",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/concurrent/CompletableFuture;supplyAsync(Ljava/util/function/Supplier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"),
            index = 0)
    private Supplier<ChunkPos> toroidal$searchOnThisWorldsNoise(Supplier<ChunkPos> search,
            @Local(index = toroidal$RING_POSITION_SLOT) int ringIndex,
            @Local(index = toroidal$RING_CHUNK_X_SLOT) int ringChunkX,
            @Local(index = toroidal$RING_CHUNK_Z_SLOT) int ringChunkZ) {
        WorldLoopTransformer transformer = this.toroidal$transformer;
        boolean beyondSearchReach = ringIndex > 0
                && transformer.chunks.overshoot(ringChunkX, ringChunkZ) > toroidal$BIOME_SEARCH_REACH_CHUNKS;
        if (beyondSearchReach) {
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
