package com.toroidalworld.mixin;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.FramedStructureStart;
import com.toroidalworld.core.WorldFold;
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

        WorldFold transformer = ShapedChunkGenerator.wrappedTransformerOf((ChunkGenerator) (Object) this);
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

    @Unique
    private static @Nullable StructureStart toroidal$inFrameOf(
            WorldGenLevel level, WorldFold transformer, ChunkPos centerPos, StructureStart start) {
        if (!start.isValid()) {
            return start;
        }

        ChunkPos startPos = start.getChunkPos();
        ChunkPos nearest = transformer.nearestCopy(centerPos, startPos);
        return ((FramedStructureStart) (Object) start).toroidal$framedBy(
                level, nearest.x - startPos.x, nearest.z - startPos.z);
    }
}
