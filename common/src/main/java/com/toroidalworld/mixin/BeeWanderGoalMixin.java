package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.phys.Vec3;

// A bee that has drifted away from its hive does not wander freely: its wander direction is aimed back at the hive, and
// that aim is a plain difference between two absolute positions — the same defect the RandomPos family had. Across the
// seam it points the long way round, so the one force meant to bring the bee home pushes it away instead.
//
// This is the caller the earlier sweep missed: the other users of the air wanderers hand them the mob's own view vector,
// which knows nothing of the seam, while this one builds a direction out of positions.
@Mixin(targets = "net.minecraft.world.entity.animal.bee.Bee$BeeWanderGoal")
public class BeeWanderGoalMixin {
    @Shadow(aliases = "this$0")
    @Final
    private Bee bee;

    @ModifyExpressionValue(
            method = "findPos",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;atCenterOf(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$hiveBiasThroughSeam(Vec3 hiveCenter) {
        return SeamSteering.nearestCopy(this.bee, hiveCenter);
    }
}
