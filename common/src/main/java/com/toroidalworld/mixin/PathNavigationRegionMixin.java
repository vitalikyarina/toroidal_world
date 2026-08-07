package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;

// Every walkability read a pathfind makes funnels through this one snapshot: it grabs the chunks in a cube around the mob
// and answers the whole A* search from them. Past the seam those chunks are ungenerated, so the snapshot stores an empty
// (all-air) chunk there and the search finds no floor to step on — a job site a few blocks across the seam is judged
// unreachable and the villager abandons it.
//
// The fold is one line, and at chunk granularity: the bounds are chunk-aligned, so a chunk index past the seam maps to
// the real chunk on the opposite side, and a block read against it lands on the same in-chunk offset (x & 15) either way.
// So the phantom slot is filled with the real opposite-side chunk, stored under its raw index, and every downstream read
// — block, fluid, collision — comes back correct with nothing else to change.
@Mixin(PathNavigationRegion.class)
public class PathNavigationRegionMixin {
    @Shadow
    @Final
    protected Level level;

    @WrapOperation(
            method = "<init>",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkSource;getChunkNow(II)Lnet/minecraft/world/level/chunk/LevelChunk;"))
    private @Nullable LevelChunk toroidal$foldSnapshotChunk(ChunkSource chunkSource, int chunkX, int chunkZ,
            Operation<@Nullable LevelChunk> original) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(this.level);
        if (transformer == null) {
            return original.call(chunkSource, chunkX, chunkZ);
        }

        return original.call(chunkSource, transformer.chunks.x.wrap(chunkX), transformer.chunks.z.wrap(chunkZ));
    }
}
