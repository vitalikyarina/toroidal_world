package com.toroidalworld.mixin;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.FramedStructureStart;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.gen.ShapedChunkGenerator;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

// A chunk places its slice of a structure by clipping the start's pieces to its own box. For a structure reached across
// the seam that clip can never hit: the start was generated in its own frame, a whole world away in raw coordinates, so
// the far half of a straddling village lands nowhere and the build comes out cut in two.
//
// The piece cannot be dragged to the chunk — a write that far is outside the region the task may touch, and widening
// that reach is the task storm the first attempt at cross-seam decoration died of. So the structure is handed to the
// chunk in the chunk's OWN frame instead: a copy of the start, moved by the whole world widths that separate the two.
//
// The substitution is made where the starts are FETCHED, not where they are placed. The place call sits inside a lambda,
// which compiles to a synthetic method that a handler scoped to applyBiomeDecoration matches not at all (conventions.md
// warns of exactly this); the fetch is in the method body, and pre-framing the list there reaches the same placement
// with nothing to say about lambdas.
//
// The copy is real, not a view. The live start serves the near half of the very same structure and worldgen runs on
// several threads, so moving its pieces in place would corrupt the half that was already correct. Serialising and
// reloading is what yields pieces of our own to move; the reloaded start has never had its bounding box asked for, so
// the lazy cache is still empty when the move happens and recomputes against the moved pieces.
@Mixin(ChunkGenerator.class)
public class ChunkGeneratorStructurePlacementMixin {
    @WrapOperation(
            method = "applyBiomeDecoration",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/StructureManager;startsForStructure(Lnet/minecraft/core/SectionPos;Lnet/minecraft/world/level/levelgen/structure/Structure;)Ljava/util/List;"))
    private List<StructureStart> toroidal$startsInTheChunksOwnFrame(
            StructureManager structureManager,
            SectionPos sectionPos,
            Structure structure,
            Operation<List<StructureStart>> original,
            @Local(argsOnly = true) WorldGenLevel level) {
        List<StructureStart> starts = original.call(structureManager, sectionPos, structure);

        WorldLoopTransformer transformer = ShapedChunkGenerator.wrappedTransformerOf((ChunkGenerator) (Object) this);
        if (transformer == null || starts.isEmpty()) {
            return starts;
        }

        ChunkPos centerPos = sectionPos.chunk();
        List<StructureStart> framed = new ArrayList<>(starts.size());
        for (StructureStart start : starts) {
            StructureStart inFrame = toroidal$inFrameOf(level, transformer, centerPos, start);
            if (inFrame != null) {
                framed.add(inFrame);
            }
        }

        return framed;
    }

    // The start as this chunk should see it: itself when the two already share a frame, its moved view when the nearest
    // copy of its own chunk lies a world away. The view is built and kept by the start (StructureStartMixin), so the
    // whole far half of one structure is placed from a single set of pieces instead of a fresh copy per chunk.
    @Unique
    private static @Nullable StructureStart toroidal$inFrameOf(
            WorldGenLevel level, WorldLoopTransformer transformer, ChunkPos centerPos, StructureStart start) {
        if (!start.isValid()) {
            return start;
        }

        ChunkPos startPos = start.getChunkPos();
        return ((FramedStructureStart) (Object) start).toroidal$framedBy(
                level,
                transformer.chunks.x.unwrap(centerPos.x, startPos.x) - startPos.x,
                transformer.chunks.z.unwrap(centerPos.z, startPos.z) - startPos.z);
    }
}
