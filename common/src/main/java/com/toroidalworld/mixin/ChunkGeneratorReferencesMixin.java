package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.toroidalworld.core.CoordinateConstants;
import com.toroidalworld.core.WorldFold;
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

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorReferencesMixin {
    // Vanilla's own scan reach, restated here because the loop it lives in is.
    @Unique
    private static final int toroidal$REFERENCE_RANGE = 8;

    @WrapMethod(method = "createReferences")
    private void toroidal$referencesThroughSeam(WorldGenLevel level, StructureManager structureManager,
            ChunkAccess centerChunk, Operation<Void> original) {
        WorldFold transformer = ShapedChunkGenerator.wrappedTransformerOf((ChunkGenerator) (Object) this);
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
                ChunkPos source = new ChunkPos(sourceX, sourceZ);
                if (!transformer.nearestCopy(centerPos, transformer.fold(source)).equals(source)) {
                    continue;
                }

                ChunkAccess sourceChunk = level.getChunk(sourceX, sourceZ);
                ChunkPos sourcePos = sourceChunk.getPos();

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
