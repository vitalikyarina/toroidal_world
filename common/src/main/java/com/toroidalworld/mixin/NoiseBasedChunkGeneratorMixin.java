package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.toroidalworld.core.ShapedChunkGenerator;
import com.toroidalworld.engine.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

@Mixin(NoiseBasedChunkGenerator.class)
public class NoiseBasedChunkGeneratorMixin {
    @WrapMethod(method = "doFill")
    private ChunkAccess toroidal$bindWhileFilling(
            Blender blender,
            StructureManager structureManager,
            RandomState randomState,
            ChunkAccess centerChunk,
            int cellYMin,
            int cellCountY,
            Operation<ChunkAccess> original) {
        return GenerationTransformerContext.withTransformer(
                ShapedChunkGenerator.transformerOf((NoiseBasedChunkGenerator) (Object) this),
                () -> original.call(blender, structureManager, randomState, centerChunk, cellYMin, cellCountY));
    }

    @WrapMethod(method = "doCreateBiomes")
    private void toroidal$bindWhileCreatingBiomes(
            Blender blender,
            RandomState randomState,
            StructureManager structureManager,
            ChunkAccess protoChunk,
            Operation<Void> original) {
        GenerationTransformerContext.runWithTransformer(
                ShapedChunkGenerator.transformerOf((NoiseBasedChunkGenerator) (Object) this),
                () -> original.call(blender, randomState, structureManager, protoChunk));
    }
}
