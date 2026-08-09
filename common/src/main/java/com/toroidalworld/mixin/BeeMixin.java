package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Bee;

// A bee measures everything it remembers — its hive, its flower — as a plain distance between two block positions, and
// both of those positions are stored inside the world. Across the seam that reads about a world apart, and the readings
// it feeds are not merely approach hints: at 48 blocks the bee stops believing the hive is reachable, and the goal that
// notices does not wait — it calls dropHive, so the bee forgets which hive is its own and the hive stops being worked.
// A bee will therefore cross the seam to pollinate and can never come back.
//
// One fold, on the one method the seven readings share: the remembered position becomes the copy nearest the bee, so
// every range gate above it measures the ground the bee would actually have to fly. A position on this side arrives
// unchanged and every reading keeps its exact vanilla answer.
@Mixin(Bee.class)
public class BeeMixin {
    @ModifyVariable(method = "closerThan(Lnet/minecraft/core/BlockPos;I)Z", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$rememberedPosThroughSeam(BlockPos targetPos) {
        return SeamSteering.nearestCopy((Bee) (Object) this, targetPos);
    }

    // The approach step is narrowed once the bee is within 15 blocks of what it is heading for. Read raw across the
    // seam that threshold is never met, so the last stretch home stays as coarse as the first.
    @ModifyExpressionValue(
            method = "pathfindRandomlyTowards",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;distManhattan(Lnet/minecraft/core/Vec3i;)I"))
    private int toroidal$stepDistanceThroughSeam(int distance, @Local(argsOnly = true) BlockPos targetPos) {
        WorldLoopTransformer transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        if (transformer == null) {
            return distance;
        }

        BlockPos beePos = ((Bee) (Object) this).blockPosition();
        return Math.abs(transformer.coords.x.foldDelta(targetPos.getX() - beePos.getX()))
                + Math.abs(targetPos.getY() - beePos.getY())
                + Math.abs(transformer.coords.z.foldDelta(targetPos.getZ() - beePos.getZ()));
    }
}
