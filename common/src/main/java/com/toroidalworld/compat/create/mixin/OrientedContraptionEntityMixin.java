package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.toroidalworld.compat.create.CreateTrackFold;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "com.simibubi.create.content.contraptions.OrientedContraptionEntity", remap = false)
public class OrientedContraptionEntityMixin {
    @ModifyExpressionValue(method = "updateOrientation",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/vehicle/AbstractMinecart;position()Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 1))
    private Vec3 toroidal$coupledCartInTheLeadingFrame(Vec3 coupledVec, @Local(index = 6) Vec3 positionVec) {
        Entity self = (Entity) (Object) this;
        return CreateTrackFold.nearestCopy(self.level(), positionVec, coupledVec);
    }
}
