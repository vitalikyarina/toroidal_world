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

@Mixin(ChunkGeneratorStructureState.class)
public abstract class ChunkGeneratorStructureStateMixin implements TransformerHolder {
    // Vanilla searches a radius of 112 blocks, so the answer lies in [centre*16 - 104, centre*16 + 120].
    @Unique
    private static final int toroidal$BIOME_SEARCH_REACH_CHUNKS = 7;

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
