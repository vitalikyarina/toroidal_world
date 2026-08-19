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
