package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.engine.seam.SeamSteering;

import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.phys.Vec3;

@Mixin(Breeze.class)
public class BreezeMixin {
    @ModifyVariable(method = "withinInnerCircleRange", at = @At("HEAD"), argsOnly = true)
    private Vec3 toroidal$innerRingTargetThroughSeam(Vec3 target) {
        return SeamSteering.nearestCopy((Breeze) (Object) this, target);
    }
}
