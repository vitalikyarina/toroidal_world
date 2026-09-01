package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.LivingEntity;

@Mixin(targets = "net.minecraft.world.entity.monster.illager.Evoker$EvokerAttackSpellGoal")
public class EvokerFangsAimMixin {
    @WrapOperation(
            method = "performSpellCasting",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;atan2(DD)D"))
    private double toroidal$fangAngleThroughSeam(double deltaZ, double deltaX, Operation<Double> original,
            @Local LivingEntity target) {
        return original.call(SeamAim.foldZ(target, deltaZ), SeamAim.foldX(target, deltaX));
    }
}
