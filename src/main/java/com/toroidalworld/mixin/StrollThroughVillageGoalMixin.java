package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.StrollThroughVillageGoal;
import net.minecraft.world.phys.Vec3;

// A mob strolling to a far corner of its village does not walk straight there: while it is more than ten blocks out it
// repeatedly aims at a point a tenth of the way along, so the walk bends around what is in the way. Both halves of that
// are raw differences, and the seam breaks them in different ways.
//
// The gate that decides whether the mob is still far away is a BlockPos reading, which none of the entity distance
// folds reach — a mob standing on the far corner reads it a world out and keeps re-aiming forever. The two differences
// that build the intermediate point are then taken between the mob and a target in the other copy of the world, so the
// waypoint they produce is behind the mob rather than in front of it.
//
// Both fold on one reading. The gate restates vanilla's own comparison — the same block centre, the same squared
// threshold — on the distance through the seam. The stroll target becomes its copy nearest the mob the once, before
// either subtraction, and vanilla's own arithmetic carries the right sign from there.
@Mixin(StrollThroughVillageGoal.class)
public class StrollThroughVillageGoalMixin {
    @Shadow
    @Final
    private PathfinderMob mob;

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"))
    private boolean toroidal$strollGateThroughSeam(BlockPos wantedPos, Position bodyPosition, double distance,
            Operation<Boolean> original) {
        return SeamRange.closerToCenterThan(this.mob, wantedPos, bodyPosition, distance);
    }

    @ModifyExpressionValue(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;atBottomCenterOf(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$strollTargetThroughSeam(Vec3 strollTarget) {
        return SeamAim.nearestTo(this.mob, strollTarget);
    }
}
