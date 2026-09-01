package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.FollowMobGoal;
import net.minecraft.world.phys.Vec3;

@Mixin(FollowMobGoal.class)
public class FollowMobGoalMixin {
    @Shadow
    @Final
    private Mob mob;

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;getX()D"))
    private double toroidal$followedXThroughSeam(Mob read, Operation<Double> original) {
        double x = original.call(read);
        return read == this.mob ? x : SeamAim.nearestTo(this.mob, read.position()).x;
    }

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;getZ()D"))
    private double toroidal$followedZThroughSeam(Mob read, Operation<Double> original) {
        double z = original.call(read);
        return read == this.mob ? z : SeamAim.nearestTo(this.mob, read.position()).z;
    }

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/control/LookControl;getWantedX()D"))
    private double toroidal$wantedLookXThroughSeam(LookControl lookControl, Operation<Double> original) {
        return toroidal$wantedNearTheMob(original.call(lookControl), lookControl.getWantedZ()).x;
    }

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/control/LookControl;getWantedZ()D"))
    private double toroidal$wantedLookZThroughSeam(LookControl lookControl, Operation<Double> original) {
        return toroidal$wantedNearTheMob(lookControl.getWantedX(), original.call(lookControl)).z;
    }

    @Unique
    private Vec3 toroidal$wantedNearTheMob(double wantedX, double wantedZ) {
        return SeamAim.nearestTo(this.mob, new Vec3(wantedX, this.mob.getY(), wantedZ));
    }
}
