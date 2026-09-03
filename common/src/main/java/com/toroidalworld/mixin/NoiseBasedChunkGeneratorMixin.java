package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.gen.ShapedChunkGenerator;
import com.toroidalworld.noise.GenerationTransformerContext;
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
        return GenerationTransformerContext.withTransformer(toroidal$transformer(),
                () -> original.call(blender, structureManager, randomState, centerChunk, cellYMin, cellCountY));
    }

    @WrapMethod(method = "doCreateBiomes")
    private void toroidal$bindWhileCreatingBiomes(
            Blender blender,
            RandomState randomState,
            StructureManager structureManager,
            ChunkAccess protoChunk,
            Operation<Void> original) {
        GenerationTransformerContext.runWithTransformer(toroidal$transformer(),
                () -> original.call(blender, randomState, structureManager, protoChunk));
    }

    @Unique
    private WorldFold toroidal$transformer() {
        WorldFold transformer = ShapedChunkGenerator.wrappedTransformerOf((NoiseBasedChunkGenerator) (Object) this);
        return transformer == null ? WorldFolds.NOOP : transformer;
    }
}
