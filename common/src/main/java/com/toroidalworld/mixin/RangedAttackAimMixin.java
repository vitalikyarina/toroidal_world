package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Illusioner;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Drowned;

@Mixin({AbstractSkeleton.class, Illusioner.class, Drowned.class, SnowGolem.class, Witch.class, WitherBoss.class})
public class RangedAttackAimMixin {
    @WrapOperation(
            method = "performRangedAttack",
            at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_X))
    private double toroidal$aimTargetX(LivingEntity target, Operation<Double> original) {
        return SeamAim.nearestTo((Entity) (Object) this,
                target.position().with(Direction.Axis.X, original.call(target))).x;
    }

    @WrapOperation(
            method = "performRangedAttack",
            at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_Z))
    private double toroidal$aimTargetZ(LivingEntity target, Operation<Double> original) {
        return SeamAim.nearestTo((Entity) (Object) this,
                target.position().with(Direction.Axis.Z, original.call(target))).z;
    }
}
