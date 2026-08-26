package com.toroidalworld.mixin;

import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.gen.SectorGridAxis;
import com.toroidalworld.gen.ShapedChunkGenerator;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorRandomSpreadSearchMixin {
    @Unique
    private static final String NEAREST_GENERATED_STRUCTURE =
            "Lnet/minecraft/world/level/chunk/ChunkGenerator;getNearestGeneratedStructure(Ljava/util/Set;Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/world/level/StructureManager;IIIZJLnet/minecraft/world/level/levelgen/structure/placement/RandomSpreadStructurePlacement;)Lcom/mojang/datafixers/util/Pair;";

    @WrapOperation(method = "findNearestMapStructure", at = @At(value = "INVOKE", target = NEAREST_GENERATED_STRUCTURE))
    private @Nullable Pair<BlockPos, Holder<Structure>> toroidal$ringsThroughTheSeam(
            Set<Holder<Structure>> structures,
            LevelReader level,
            StructureManager structureManager,
            int chunkOriginX,
            int chunkOriginZ,
            int radius,
            boolean createReference,
            long seed,
            RandomSpreadStructurePlacement placement,
            Operation<@Nullable Pair<BlockPos, Holder<Structure>>> original) {
        WorldFold transformer = ShapedChunkGenerator.wrappedTransformerOf((ChunkGenerator) (Object) this);
        if (transformer == null) {
            return original.call(structures, level, structureManager, chunkOriginX, chunkOriginZ, radius,
                    createReference, seed, placement);
        }

        SectorGridAxis xCells =
                SectorGridAxis.of(transformer.chunkDomain(Direction.Axis.X), placement.spacing(), chunkOriginX);
        SectorGridAxis zCells =
                SectorGridAxis.of(transformer.chunkDomain(Direction.Axis.Z), placement.spacing(), chunkOriginZ);
        if (radius > Math.max(xCells.offsetCap(), zCells.offsetCap())) {
            return null;
        }

        for (int x = -radius; x <= radius; x++) {
            if (Math.abs(x) > xCells.offsetCap()) {
                continue;
            }

            boolean xEdge = x == -radius || x == radius;
            for (int z = -radius; z <= radius; z++) {
                if (!xEdge && z != -radius && z != radius) {
                    continue;
                }

                if (Math.abs(z) > zCells.offsetCap()) {
                    continue;
                }

                ChunkPos candidate = placement.getPotentialStructureChunk(seed, xCells.probeChunk(x), zCells.probeChunk(z));
                if (transformer.isOver(candidate)) {
                    continue;
                }

                Pair<BlockPos, Holder<Structure>> generating = ChunkGeneratorAccessor.toroidal$structureGeneratingAt(
                        structures, level, structureManager, createReference, placement, candidate);
                if (generating != null) {
                    return generating;
                }
            }
        }

        return null;
    }

    @WrapOperation(
            method = "findNearestMapStructure",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;distSqr(Lnet/minecraft/core/Vec3i;)D"))
    private double toroidal$rankThroughTheSeam(BlockPos origin, Vec3i candidate, Operation<Double> original) {
        WorldFold transformer = ShapedChunkGenerator.wrappedTransformerOf((ChunkGenerator) (Object) this);
        if (transformer == null) {
            return original.call(origin, candidate);
        }

        return transformer.sqrDistance(
                origin.getX(), origin.getY(), origin.getZ(), candidate.getX(), candidate.getY(), candidate.getZ());
    }
}
