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

    @Unique
    private WorldLoopTransformer toroidal$transformer() {
        return this.toroidal$resolvedTransformer();
    }

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
