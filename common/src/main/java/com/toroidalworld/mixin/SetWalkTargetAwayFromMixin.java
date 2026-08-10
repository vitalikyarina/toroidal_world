package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.toroidalworld.entity.SeamRange;
import com.toroidalworld.probe.ReseatProbe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.Position;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetAwayFrom;
import net.minecraft.world.phys.Vec3;

// Fleeing begins with "is the thing close enough to be worth running from?", and only past that gate does the escape
// route get worked out. The route itself already crosses the seam correctly, which is exactly why this reading matters:
// the gate stands in front of it, so from the wrong side of the boundary a threat two blocks away reads a world off and
// the mob never starts running at all. A villager stays put beside the zombie that is about to kill it.
@Mixin(SetWalkTargetAwayFrom.class)
public class SetWalkTargetAwayFromMixin {
    @WrapOperation(
            method = "*",
            require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;closerThan(Lnet/minecraft/core/Position;D)Z"))
    private static boolean toroidal$avoidReachThroughSeam(Vec3 bodyPosition, Position avoidPosition, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) PathfinderMob body) {
        return ReseatProbe.decided(body.level(), ReseatProbe.AVOID_REACH,
                original.call(bodyPosition, avoidPosition, distance),
                SeamRange.closerThan(body, bodyPosition, avoidPosition, distance));
    }

    // Past the gate the behaviour refuses to re-plan a flight already heading the right way, and it asks that as the dot
    // product of two headings: the way the current walk is going, and the way the threat lies. Both are raw differences
    // from the mob, so across the seam both come out reversed — the product keeps its sign and the answer looks right by
    // accident, until only one of the two crosses the boundary and the mob then abandons a good escape or keeps a
    // useless one. Each difference is folded where it is taken; the dot vanilla computes from them is untouched.
    @WrapOperation(
            method = "*",
            require = 2,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 toroidal$avoidHeadingThroughSeam(Vec3 from, Vec3 to, Operation<Vec3> original,
            @Local(argsOnly = true) PathfinderMob body) {
        Vec3 vanilla = original.call(from, to);
        return ReseatProbe.decided(body.level(), ReseatProbe.AVOID_HEADING, vanilla,
                SeamAim.foldDelta(body, vanilla));
    }
}
