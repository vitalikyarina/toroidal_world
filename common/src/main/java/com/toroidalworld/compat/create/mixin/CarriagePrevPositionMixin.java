package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.toroidalworld.compat.create.CreateTrackFold;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "com.simibubi.create.content.trains.entity.CarriageContraptionEntity", remap = false)
public abstract class CarriagePrevPositionMixin {
    @ModifyReturnValue(method = "getPrevPositionVec", at = @At("RETURN"))
    private Vec3 toroidal$prevPositionInCurrentFrame(Vec3 previous) {
        Entity carriage = (Entity) (Object) this;
        return CreateTrackFold.nearestCopy(carriage.level(), carriage.position(), previous);
    }
}
