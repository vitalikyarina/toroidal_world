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

// A patrol does not walk at its target in a straight line. It takes the difference to the target, turns it a quarter
// turn, and steps ten blocks along the result — which is what makes a patrol arrive from the side and spread out rather
// than file in. The leader picks the next target once it is within ten blocks of this one.
//
// Both readings are raw, and one feeds the other: the arrival never fires, so the target is never replaced, and the
// flanking step is built from a difference pointing the long way round — the patrol walks away from the point it is
// bound for, forever, since nothing can end the leg.
//
// The target is read twice in the tick and folded at both, so the gate measures the ground the patrol would cover and
// the quarter turn is taken on the short difference. What the leader stores when it does finally pick a new target is
// its own business, and is left alone.
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

    // The flanking step is where the leader tells the rest of the patrol where the leg ends, and the point it hands
    // over is built from the folded target ten blocks out — which near the boundary lands past it. Steering may hold a
    // position outside the bounds, because it is spent within the tick that made it; a patrol target is not steering.
    // It is written into another mob and saved with it, to be read back by a mob that may be nowhere near where the
    // leader stood, and no coordinate the server keeps is allowed to name a place the world does not have.
    //
    // Only the copy that leaves the goal is brought back inside. What the navigation is given stays as it was — it
    // unwraps toward the mob anyway, and that is the direction the leader means.
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
