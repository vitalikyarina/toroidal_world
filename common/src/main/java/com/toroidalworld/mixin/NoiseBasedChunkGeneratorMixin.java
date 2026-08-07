package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.gen.ShapedChunkGenerator;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

// Six of the eight chunk steps sample on the thread that runs them, so binding the transformer at the step is enough
// (ChunkStatusTasksMixin). The other two — noise fill and biomes — hand their work to a background executor, and these
// two private methods are what that executor actually runs.
//
// Binding here rather than at the step is what lets the holder stay thread-local: the transformer never has to cross a
// thread, so two dimensions generating at once cannot read each other's width. Reaching it needs nothing new either —
// the generator whose method this is already carries its own bounds.
//
// The binding is unconditional — NOOP for a plain generator — and not skipped when this is not a looped one. These
// two methods run on a shared background executor, and the synchronous steps bind through set(), which leaves its value
// on the thread rather than restoring it. So a thread that last ran a looped world's step still carries that world's
// transformer; a plain generator that skipped the bind would sample against it and produce periodic terrain in an
// ordinary world (looped world created, then a normal one, same session). Binding NOOP scoped overwrites the
// leftover and restores it, leaving the thread as clean as it found it.
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
    private WorldLoopTransformer toroidal$transformer() {
        return (Object) this instanceof ShapedChunkGenerator shaped ? shaped.transformer() : WorldLoopTransformer.NOOP;
    }
}
