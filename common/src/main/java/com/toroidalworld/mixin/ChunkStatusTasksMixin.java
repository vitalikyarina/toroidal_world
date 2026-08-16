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

// Binds the level's transformer around every generation step — scoped, for every level, NOOP included. The steps
// run on the shared background pool, and that pool also runs samplers with no binder of their own (the stronghold ring
// search among them): a binding left on the thread would be read by whatever lands there next, so each step restores
// what it found. An unwrapped level's step binding NOOP is not redundant either — it is what shields the step from
// someone else's leftover ever mattering again.
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
