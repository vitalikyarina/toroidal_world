package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.phys.Vec3;

// A bee that has drifted away from its hive does not wander freely: its wander direction is aimed back at the hive, and
// that aim is a plain difference between two absolute positions — the same defect the RandomPos family had. Across the
// seam it points the long way round, so the one force meant to bring the bee home pushes it away instead.
//
// This is the caller the earlier sweep missed: the other users of the air wanderers hand them the mob's own view vector,
// which knows nothing of the seam, while this one builds a direction out of positions.
@Mixin(targets = "net.minecraft.world.entity.animal.Bee$BeeWanderGoal")
public class BeeWanderGoalMixin {
    // From the constructor, not shadowed off this$0 — see BeeEnterHiveGoalMixin: the outer reference is javac's, not
    // any mapping set's, so a remapping loader has nothing to resolve it to.
    @Unique
    private Bee toroidal$bee;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/animal/Bee;)V", at = @At("TAIL"))
    private void toroidal$captureBee(Bee bee, CallbackInfo ci) {
        this.toroidal$bee = bee;
    }

    @ModifyExpressionValue(
            method = "findPos",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;atCenterOf(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$hiveBiasThroughSeam(Vec3 hiveCenter) {
        return SeamSteering.nearestCopy(this.toroidal$bee, hiveCenter);
    }
}
