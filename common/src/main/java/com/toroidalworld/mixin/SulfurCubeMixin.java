package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.toroidalworld.InjectionTargets;
import com.toroidalworld.entity.SeamAim;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.phys.Vec3;

@Mixin(SulfurCube.class)
public class SulfurCubeMixin {
    @ModifyVariable(
            method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double toroidal$knockbackDirX(double xd) {
        return SeamAim.foldX((Entity) (Object) this, xd);
    }

    @ModifyVariable(
            method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private double toroidal$knockbackDirZ(double zd) {
        return SeamAim.foldZ((Entity) (Object) this, zd);
    }

    @ModifyExpressionValue(
            method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V",
            at = @At(value = "INVOKE",
                    target = InjectionTargets.ENTITY_GET_EYE_POSITION))
    private Vec3 toroidal$attackerEyeThroughSeam(Vec3 attackerEye) {
        return SeamAim.nearestTo((Entity) (Object) this, attackerEye);
    }

    @ModifyExpressionValue(
            method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V",
            at = @At(value = "INVOKE",
                    target = InjectionTargets.ENTITY_POSITION))
    private Vec3 toroidal$attackerFeetThroughSeam(Vec3 attackerFeet) {
        return SeamAim.nearestTo((Entity) (Object) this, attackerFeet);
    }
}
