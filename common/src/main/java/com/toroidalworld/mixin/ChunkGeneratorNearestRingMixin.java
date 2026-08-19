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
