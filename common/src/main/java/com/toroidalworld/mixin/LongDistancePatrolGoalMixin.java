package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.PatrollingMonster;

@Mixin(PatrollingMonster.LongDistancePatrolGoal.class)
public class LongDistancePatrolGoalMixin {
    @Shadow
    @Final
    private PatrollingMonster mob;

    @ModifyExpressionValue(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/monster/PatrollingMonster;"
                            + "getPatrolTarget()Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$patrolTargetThroughSeam(BlockPos patrolTarget) {
        return SeamSteering.nearestCopy(this.mob, patrolTarget);
    }

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/monster/PatrollingMonster;"
                            + "setPatrolTarget(Lnet/minecraft/core/BlockPos;)V"))
    private void toroidal$companionTargetInBounds(PatrollingMonster companion, BlockPos legEnd,
            Operation<Void> original) {
        WorldLoopTransformer transformer = ((TransformerSource) this.mob).toroidal$wrappedTransformer();
        original.call(companion, transformer == null ? legEnd : transformer.blocks.wrap(legEnd));
    }
}
