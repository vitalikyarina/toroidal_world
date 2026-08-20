package com.toroidalworld.mixin;

import java.util.concurrent.CompletableFuture;

import org.spongepowered.asm.mixin.Mixin;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.PeriodicityCheck;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatusTasks;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.WorldGenContext;

@Mixin(ChunkStatusTasks.class)
public class ChunkStatusTasksMixin {
    @WrapMethod(
            method = {
                    "generateStructureStarts",
                    "generateStructureReferences",
                    "generateBiomes",
                    "generateNoise",
                    "generateSurface",
                    "generateCarvers",
                    "generateFeatures",
                    "generateSpawn",
                    "initializeLight",
                    "light"
            })
    private static CompletableFuture<ChunkAccess> toroidal$bindTransformer(
            WorldGenContext context,
            ChunkStep step,
            StaticCache2D<GenerationChunkHolder> chunks,
            ChunkAccess chunk,
            Operation<CompletableFuture<ChunkAccess>> original) {
        WorldLoopTransformer transformer = WorldLoopAttachments.transformerOf(context.level());
        if (transformer.isWrapped()) {
            PeriodicityCheck.runOnce(context.level(), transformer);
        }

        return GenerationTransformerContext.withTransformer(transformer,
                () -> original.call(context, step, chunks, chunk));
    }
}
