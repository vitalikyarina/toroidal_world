package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.LevelBindable;
import com.toroidalworld.accessors.TransformerCache;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.server.level.ChunkTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

@Mixin(ChunkTracker.class)
public class ChunkTrackerMixin implements LevelBindable, TransformerCache {
    @Unique
    private @Nullable ServerLevel toroidal$level;

    @Unique
    private @Nullable WorldFold toroidal$boundTransformer;

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
        WorldFold transformer = this.toroidal$transformer();
        if (!transformer.isWrapped()) {
            return original.call(chunkX, chunkZ);
        }

        long folded = transformer.foldChunkKey(ChunkPos.asLong(chunkX, chunkZ));
        return original.call(ChunkPos.getX(folded), ChunkPos.getZ(folded));
    }

    @Override
    public WorldFold toroidal$transformer() {
        WorldFold transformer = this.toroidal$boundTransformer;
        if (transformer != null) {
            return transformer;
        }

        if (this.toroidal$level == null) {
            return WorldFolds.NOOP;
        }

        transformer = WorldLoopAttachments.transformerOf(this.toroidal$level);
        this.toroidal$boundTransformer = transformer;
        return transformer;
    }
}
