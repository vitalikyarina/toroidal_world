package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.gen.ShapedChunkGenerator;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.chunk.ChunkGenerator;

// The ring list is already folded into the world (ChunkGeneratorStructureStateMixin), but the concentric locate branch
// still picks the closest ring position by raw distSqr on canonical coordinates. On a torus a stronghold just across
// the seam reads as half a world away and loses to one much farther the flat way — /locate names the wrong stronghold
// and the eye of ender is signalled toward it. The candidate distance is measured through the seam instead; on a world
// with a single surviving ring position the fold changes which number is reported, not which stronghold wins.
@Mixin(ChunkGenerator.class)
public class ChunkGeneratorNearestRingMixin {
    @Unique
    private static final String NEAREST_RING_STRUCTURE =
            "getNearestGeneratedStructure(Ljava/util/Set;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/level/levelgen/structure/placement/ConcentricRingsStructurePlacement;)Lcom/mojang/datafixers/util/Pair;";

    @WrapOperation(
            method = NEAREST_RING_STRUCTURE,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos$MutableBlockPos;distSqr(Lnet/minecraft/core/Vec3i;)D"))
    private double toroidal$ringDistThroughSeam(BlockPos.MutableBlockPos candidate, Vec3i origin,
            Operation<Double> original) {
        WorldLoopTransformer transformer = ShapedChunkGenerator.wrappedTransformerOf((ChunkGenerator) (Object) this);
        if (transformer == null) {
            return original.call(candidate, origin);
        }

        return transformer.coords.sqrDistToBounds(
                origin.getX(), origin.getY(), origin.getZ(), candidate.getX(), candidate.getY(), candidate.getZ());
    }
}
