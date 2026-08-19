package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.toroidalworld.core.CoordinateConstants;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.gen.ShapedChunkGenerator;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.StructureStart;

// A chunk learns which structures reach into it by scanning a square of neighbours for starts whose bounding box covers
// it. Past the bounds that square names start-less phantoms, while the real chunks across the seam are never scanned —
// so a structure straddling the boundary is never referenced by its far half, and that half places nothing.
//
// The scan stays on RAW names — the task's cache is built over the raw square and holds nothing else — and leans on the
// slot fold ChunkGenerationTaskMixin has already applied, which is what turns an out-of-bounds slot into the real chunk
// across the seam wherever a companion ticket makes that legal. Restating the loop is still needed for two things:
//
// - **One copy per physical chunk.** The scan is 17 chunks wide and a looped world may be 16, so the same physical chunk
//   can appear under several raw names — one start referenced repeatedly, each under a different shift, placing the
//   structure on top of itself. Only the copy nearest the centre is kept.
// - **Two frames, one comparison.** A start reached across the seam carries its box in its own chunk's frame, a world
//   away from the centre. The centre's box is restated in that frame instead (shifted by the delta the fold introduced),
//   so the intersection is measured once, in one frame, with no box allocated.
//
// The reference is filed under the key of the chunk actually handed back: that key is what
// StructureManager.fillStartsForStructure later gives getChunk to fetch the start again, and a raw key over a folded
// slot would resolve to the phantom and find nothing.
@Mixin(ChunkGenerator.class)
public class ChunkGeneratorReferencesMixin {
    // Vanilla's own scan reach, restated here because the loop it lives in is.
    @Unique
    private static final int toroidal$REFERENCE_RANGE = 8;

    @WrapMethod(method = "createReferences")
    private void toroidal$referencesThroughSeam(WorldGenLevel level, StructureManager structureManager,
            ChunkAccess centerChunk, Operation<Void> original) {
        WorldLoopTransformer transformer = ShapedChunkGenerator.wrappedTransformerOf((ChunkGenerator) (Object) this);
        if (transformer == null) {
            original.call(level, structureManager, centerChunk);
            return;
        }

        ChunkPos centerPos = centerChunk.getPos();
        int centerChunkX = centerPos.x;
        int centerChunkZ = centerPos.z;
        int centerBlockX = centerPos.getMinBlockX();
        int centerBlockZ = centerPos.getMinBlockZ();
        SectionPos centerSection = SectionPos.bottomOf(centerChunk);

        for (int sourceX = centerChunkX - toroidal$REFERENCE_RANGE; sourceX <= centerChunkX + toroidal$REFERENCE_RANGE; sourceX++) {
            for (int sourceZ = centerChunkZ - toroidal$REFERENCE_RANGE; sourceZ <= centerChunkZ + toroidal$REFERENCE_RANGE; sourceZ++) {
                if (transformer.chunks.x.unwrap(centerChunkX, transformer.chunks.x.wrap(sourceX)) != sourceX
                        || transformer.chunks.z.unwrap(centerChunkZ, transformer.chunks.z.wrap(sourceZ)) != sourceZ) {
                    continue;
                }

                // Read the slot by its RAW name, never by the physical one. The task's cache spans the raw square and
                // nothing else: asking it for the physical chunk is asking for a coordinate it was never built to hold,
                // which is vanilla's "Requested chunk unavailable" the moment the two differ by more than the square.
                // ChunkGenerationTaskMixin has already replaced the out-of-bounds slots it may — within the seam
                // neighbourhood, where a companion ticket guarantees the holder — so the raw name yields the real chunk
                // across the seam exactly where that is legal, and the phantom (no starts, nothing added) beyond it.
                ChunkAccess sourceChunk = level.getChunk(sourceX, sourceZ);
                ChunkPos sourcePos = sourceChunk.getPos();

                // Everything downstream is stated in the frame of the chunk the cache actually handed back: the shift is
                // zero and the key is the raw one whenever the slot was not folded, so an unfolded slot stays vanilla.
                int foldedCenterBlockX = centerBlockX - (sourceX - sourcePos.x) * CoordinateConstants.CHUNK_WIDTH;
                int foldedCenterBlockZ = centerBlockZ - (sourceZ - sourcePos.z) * CoordinateConstants.CHUNK_WIDTH;
                long referenceKey = sourcePos.toLong();

                for (StructureStart start : sourceChunk.getAllStarts().values()) {
                    if (start.isValid() && start.getBoundingBox().intersects(
                            foldedCenterBlockX,
                            foldedCenterBlockZ,
                            foldedCenterBlockX + CoordinateConstants.CHUNK_WIDTH - 1,
                            foldedCenterBlockZ + CoordinateConstants.CHUNK_WIDTH - 1)) {
                        structureManager.addReferenceForStructure(
                                centerSection, start.getStructure(), referenceKey, centerChunk);
                    }
                }
            }
        }
    }
}
