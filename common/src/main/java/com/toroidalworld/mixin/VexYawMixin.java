package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.phys.Vec3;

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

        Vec3 delta = SeamAim.deltaTo(vex, target.position());
        double deltaX = delta.x;
        double deltaZ = delta.z;
        original.call(vex, -((float) Mth.atan2(deltaX, deltaZ)) * (180.0F / (float) Math.PI));
    }
}
