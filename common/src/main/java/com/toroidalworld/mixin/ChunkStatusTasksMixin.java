package com.toroidalworld.mixin;

import java.util.concurrent.CompletableFuture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldLoopAttachments;
import com.toroidalworld.engine.gen.FloatingCrumbs;
import com.toroidalworld.engine.noise.GenerationTransformerContext;
import com.toroidalworld.engine.noise.PeriodicityCheck;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
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
        WorldFold transformer = WorldLoopAttachments.transformerOf(context.level());
        if (transformer.isWrapped()) {
            PeriodicityCheck.runOnce(context.level(), transformer);
        }

        return GenerationTransformerContext.withTransformer(transformer,
                () -> original.call(context, step, chunks, chunk));
    }

    @ModifyReturnValue(method = "generateCarvers", at = @At("RETURN"))
    private static CompletableFuture<ChunkAccess> toroidal$sweepFloatingCrumbs(
            CompletableFuture<ChunkAccess> original,
            WorldGenContext context,
            ChunkStep step,
            StaticCache2D<GenerationChunkHolder> chunks,
            ChunkAccess chunk) {
        return original.thenApply(carved -> {
            FloatingCrumbs.sweep(context.level(), carved);
            FloatingCrumbs.registerMask(context.level(), carved);
            return carved;
        });
    }

    @ModifyReturnValue(method = "light", at = @At("RETURN"))
    private static CompletableFuture<ChunkAccess> toroidal$sweepBorderCrumbs(
            CompletableFuture<ChunkAccess> original,
            WorldGenContext context,
            ChunkStep step,
            StaticCache2D<GenerationChunkHolder> chunks,
            ChunkAccess chunk) {
        return original.thenApply(lit -> {
            FloatingCrumbs.sweepAcross(context.level(), lit, chunks);
            return lit;
        });
    }
}
