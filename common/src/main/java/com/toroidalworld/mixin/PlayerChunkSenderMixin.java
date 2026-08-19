package com.toroidalworld.mixin;

import java.util.Comparator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.toroidalworld.accessors.LevelHolder;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

@Mixin(PlayerChunkSender.class)
public class PlayerChunkSenderMixin {
    @ModifyArg(
            method = "collectChunksToSend",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/collect/Comparators;least(ILjava/util/Comparator;)Ljava/util/stream/Collector;"),
            index = 1)
    private Comparator<Long> toroidal$nearestPendingThroughSeam(Comparator<Long> original,
            @Local(argsOnly = true) ChunkMap chunkMap, @Local(argsOnly = true) ChunkPos playerPos) {
        WorldLoopTransformer transformer = toroidal$transformer(chunkMap);
        if (!transformer.isWrapped()) {
            return original;
        }

        long packedPlayerPos = playerPos.toLong();
        return Comparator.comparingInt(pending -> transformer.chunks.sqrDistToBounds(packedPlayerPos, pending));
    }

    @ModifyArg(
            method = "collectChunksToSend",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/stream/Stream;sorted(Ljava/util/Comparator;)Ljava/util/stream/Stream;"),
            index = 0)
    private Comparator<LevelChunk> toroidal$nearestLoadedThroughSeam(Comparator<LevelChunk> original,
            @Local(argsOnly = true) ChunkMap chunkMap, @Local(argsOnly = true) ChunkPos playerPos) {
        WorldLoopTransformer transformer = toroidal$transformer(chunkMap);
        if (!transformer.isWrapped()) {
            return original;
        }

        return Comparator.comparingInt(chunk -> transformer.chunks.sqrDistToBounds(chunk.getPos(), playerPos));
    }

    @Unique
    private static WorldLoopTransformer toroidal$transformer(ChunkMap chunkMap) {
        return WorldLoopAttachments.transformerOf(((LevelHolder) chunkMap).toroidal$level());
    }
}
