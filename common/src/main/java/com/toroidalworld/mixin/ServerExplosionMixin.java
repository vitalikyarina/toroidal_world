package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.toroidalworld.InjectionTargets;
import com.toroidalworld.entity.SeamAim;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;

@Mixin(Explosion.class)
public class ServerExplosionMixin {
    @Shadow
    @Final
    private double x;

    @Shadow
    @Final
    private double z;

    @ModifyVariable(
            method = "getSeenPercent(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/Entity;)F",
            at = @At("HEAD"), argsOnly = true)
    private static Vec3 toroidal$exposureCentreThroughSeam(Vec3 centre, @Local(argsOnly = true) Entity entity) {
        Vec3 folded = SeamAim.nearestTo(entity, centre);
        return folded;
    }

    @ModifyExpressionValue(
            method = "explode",
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_X))
    private double toroidal$knockbackOriginX(double entityX, @Local Entity entity) {
        double folded = this.x + SeamAim.foldX(entity, entityX - this.x);
        return folded;
    }

    @ModifyExpressionValue(
            method = "explode",
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_Z))
    private double toroidal$knockbackOriginZ(double entityZ, @Local Entity entity) {
        double folded = this.z + SeamAim.foldZ(entity, entityZ - this.z);
        return folded;
    }
}
