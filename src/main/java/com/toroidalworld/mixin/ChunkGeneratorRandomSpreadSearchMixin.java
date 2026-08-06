package com.toroidalworld.mixin;

import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.gen.SectorGridAxis;
import com.toroidalworld.gen.ShapedChunkGenerator;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

// A structure search spirals outward in rings, and a ring steps by the placement's own spacing rather than by one
// chunk — vanilla names its cells by raw offsets from the origin's cell, which on a torus is blind in both directions
// at once. A cell just across the seam sits at a raw offset of nearly the world's width in cells, so the physically
// nearest structure is met only after rings the search radius never allows — a buried treasure three chunks past the
// seam of a 256-chunk world needs ring ~253 at spacing 1, against the 50 its loot function asks for. And when the far
// side is reachable at all, the raw distance the caller ranks results by prefers a far same-side structure to a near
// one through the seam.
//
// Both blindnesses are folded here. The ring scan is restated on the placement's sector grid folded into the world
// (SectorGridAxis): ring r names the cells r sectors away through the seam as well as the flat way, so vanilla's
// first-hit-per-ring ordering means folded-nearest, every cell of a closed axis is named by rings 0..cap, and rings
// past that cap are skipped outright — the cost of a search for something the world does not hold is capped by the
// world itself, one call per ring per placement. A candidate the grid still names past the bounds — a partial edge
// cell whose spread reaches past the world — is turned away before it is probed: generation only ever runs inside the
// bounds, so a start cannot exist there, and probing it anyway would sample the periodic noise chain for ground that
// is not there — confidently, and a yes would pull real chunk generation out of a phantom.
//
// The ranking distSqr in findNearestMapStructure is measured through the seam for the same reason: it decides between
// placements, and between the random-spread result and the concentric-rings one.
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
        WorldLoopTransformer transformer = ShapedChunkGenerator.wrappedTransformerOf((ChunkGenerator) (Object) this);
        if (transformer == null) {
            return original.call(structures, level, structureManager, chunkOriginX, chunkOriginZ, radius,
                    createReference, seed, placement);
        }

        SectorGridAxis xCells = SectorGridAxis.of(transformer.chunks.x, placement.spacing(), chunkOriginX);
        SectorGridAxis zCells = SectorGridAxis.of(transformer.chunks.z, placement.spacing(), chunkOriginZ);
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
                if (transformer.chunks.isOver(candidate)) {
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
        WorldLoopTransformer transformer = ShapedChunkGenerator.wrappedTransformerOf((ChunkGenerator) (Object) this);
        if (transformer == null) {
            return original.call(origin, candidate);
        }

        return transformer.coords.sqrDistToBounds(
                origin.getX(), origin.getY(), origin.getZ(), candidate.getX(), candidate.getY(), candidate.getZ());
    }
}
