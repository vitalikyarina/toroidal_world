package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Illusioner;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Drowned;

@Mixin({AbstractSkeleton.class, Illusioner.class, Drowned.class, SnowGolem.class, Witch.class, WitherBoss.class})
public class RangedAttackAimMixin {
    @ModifyExpressionValue(
            method = "performRangedAttack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getX()D"))
    private double toroidal$aimTargetX(double targetX) {
        return SeamAim.nearX((Entity) (Object) this, targetX);
    }

    @ModifyExpressionValue(
            method = "performRangedAttack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D"))
    private double toroidal$aimTargetZ(double targetZ) {
        return SeamAim.nearZ((Entity) (Object) this, targetZ);
    }
}
