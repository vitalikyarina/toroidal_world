package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "net.minecraft.world.entity.animal.bee.Bee$BeeWanderGoal")
public class BeeWanderGoalMixin {
    @Shadow(aliases = "this$0")
    @Final
    private Bee bee;

    @ModifyExpressionValue(
            method = "findPos",
            at = @At(value = "INVOKE",
                    target = InjectionTargets.VEC3_AT_CENTER_OF))
    private Vec3 toroidal$hiveBiasThroughSeam(Vec3 hiveCenter) {
        return SeamSteering.nearestCopy(this.bee, hiveCenter);
    }
}
