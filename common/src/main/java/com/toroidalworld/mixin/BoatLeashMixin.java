package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;

@Mixin(Boat.class)
public class BoatLeashMixin {
    @ModifyExpressionValue(
            method = "elasticRangeLeashBehaviour",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;position()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$holderPositionThroughSeam(Vec3 holderPosition) {
        Boat boat = (Boat) (Object) this;
        return SeamSteering.nearestCopy(boat, holderPosition);
    }
}
