package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.entity.SeamRange;
import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.InteractWithDoor;

@Mixin(InteractWithDoor.class)
public class InteractWithDoorMixin {
    @WrapOperation(
            method = "*",
            require = 2,
            at = @At(value = "INVOKE",
                    target = InjectionTargets.BLOCK_POS_CLOSER_TO_CENTER_THAN),
            expect = 2)
    private static boolean toroidal$doorReachThroughSeam(BlockPos doorPos, Position bodyPosition, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) LivingEntity body) {
        return SeamRange.closerToCenterThan(body, doorPos, bodyPosition, distance);
    }

    @ModifyExpressionValue(
            method = "closeDoorsThatIHaveOpenedOrPassedThrough",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/pathfinder/Node;asBlockPos()Lnet/minecraft/core/BlockPos;"))
    private static BlockPos toroidal$standingInDoorwayThroughSeam(BlockPos nodePos,
            @Local(argsOnly = true) LivingEntity body, @Local BlockPos doorPos) {
        WorldFold transformer = ((TransformerSource) body).toroidal$wrappedTransformer();
        return transformer == null ? nodePos : transformer.nearestCopy(doorPos, nodePos);
    }

    @WrapOperation(
            method = "*",
            require = 1,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/behavior/InteractWithDoor;"
                            + "isMobComingThroughDoor(Lnet/minecraft/world/entity/ai/Brain;Lnet/minecraft/core/BlockPos;)Z"))
    private static boolean toroidal$otherMobsDoorwayThroughSeam(Brain<?> otherBrain, BlockPos doorPos,
            Operation<Boolean> original, @Local(argsOnly = true) LivingEntity otherMob) {
        BlockPos nearest = SeamSteering.nearestCopy(otherMob, doorPos);
        return original.call(otherBrain, nearest);
    }
}
