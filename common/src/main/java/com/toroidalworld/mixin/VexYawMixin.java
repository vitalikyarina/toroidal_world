package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Vex;

@Mixin(targets = "net.minecraft.world.entity.monster.Vex$VexMoveControl")
public class VexYawMixin {
    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Vex;setYRot(F)V", ordinal = 1))
    private void toroidal$bodyYawThroughSeam(Vex vex, float yRot, Operation<Void> original) {
        LivingEntity target = vex.getTarget();
        if (target == null) {
            original.call(vex, yRot);
            return;
        }

        double deltaX = SeamAim.foldX(vex, target.getX() - vex.getX());
        double deltaZ = SeamAim.foldZ(vex, target.getZ() - vex.getZ());
        original.call(vex, -((float) Mth.atan2(deltaX, deltaZ)) * (180.0F / (float) Math.PI));
    }
}
