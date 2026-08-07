package com.toroidalworld.mixin;

import java.util.Comparator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.PrepareRamNearestTarget;
import net.minecraft.world.phys.Vec3;

// The four candidate run-ups are walked out from the target itself, so the distance between a candidate and the target
// is honest wherever the target stands — both sit in the one frame the walk built. What is not honest is the ordering
// that picks between them: it measures each candidate from the mob, and a mob on the far side of the seam reads all
// four a world away with their order inverted, so it charges from the worst starting point it found instead of the
// nearest.
//
// The comparator is replaced rather than the reading wrapped, because vanilla builds it from a bound method reference —
// there is no distance call in the bytecode to wrap.
@Mixin(PrepareRamNearestTarget.class)
public class PrepareRamNearestTargetMixin {
    @ModifyArg(
            method = "calculateRammingStartPosition",
            at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;sorted(Ljava/util/Comparator;)Ljava/util/stream/Stream;"),
            index = 0)
    private Comparator<BlockPos> toroidal$ramStartOrderThroughSeam(Comparator<BlockPos> original,
            @Local(argsOnly = true) PathfinderMob body) {
        BlockPos bodyPos = body.blockPosition();
        return Comparator.comparingDouble(candidate -> SeamRange.sqr(body, bodyPos, candidate));
    }

    // A run-up is walked out from the target and keeps whatever coordinate that walk arrived at, so one that steps past
    // the bounds is remembered outside the world. The mob's own position never is. Standing on that very block it then
    // compares the two names of it and finds them different, so it never registers as having arrived: the charge is
    // never armed, and the mob waits on its mark until the behaviour times out.
    //
    // The run-up is restated as the block that physically exists, which the arrival test and the walk target both then
    // agree on — and a walk target inside the world stays true when its owner crosses the seam, which one written a
    // world out would not.
    @ModifyExpressionValue(
            method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PathfinderMob;J)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/behavior/PrepareRamNearestTarget$RamCandidate;getStartPosition()Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$ramStartThroughSeam(BlockPos startPos, @Local(argsOnly = true) PathfinderMob body) {
        WorldLoopTransformer transformer = ((TransformerSource) body).toroidal$wrappedTransformer();
        if (transformer == null) {
            return startPos;
        }

        return transformer.blocks.wrap(startPos);
    }

    // Arming the charge picks which face of the target block to aim at, from the sign of the step between the mark and
    // the target. Read raw across the seam that sign is the wrong one, so the aim point lands on the near face instead
    // of the far one — half a block, but it goes straight into the memory the charge direction is taken from.
    //
    // The mark is handed in as its copy nearest the target, so vanilla's own subtraction carries the right sign; the
    // point it builds is measured from the target and stays inside the world.
    @WrapOperation(
            method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PathfinderMob;J)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/behavior/PrepareRamNearestTarget;getEdgeOfBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$ramEdgeThroughSeam(PrepareRamNearestTarget<?> self, BlockPos startRamPos, BlockPos targetPos,
            Operation<Vec3> original, @Local(argsOnly = true) PathfinderMob body) {
        WorldLoopTransformer transformer = ((TransformerSource) body).toroidal$wrappedTransformer();
        if (transformer == null) {
            return original.call(self, startRamPos, targetPos);
        }

        return original.call(self, transformer.blocks.nearestCopy(targetPos, startRamPos), targetPos);
    }
}
