package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.toroidalworld.compat.create.CreateTrackFold;

import net.minecraft.world.phys.Vec3;

@Mixin(value = Carriage.class, remap = false)
public abstract class CarriagePairMixin {
    @Shadow
    public abstract TravellingPoint getLeadingPoint();

    @WrapOperation(method = "getAnchorDiff",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;distanceTo(Lnet/minecraft/world/phys/Vec3;)D"))
    private double toroidal$foldAnchorSpan(Vec3 leading, Vec3 trailing, Operation<Double> original) {
        return original.call(leading,
                CreateTrackFold.nearestCopy(getLeadingPoint().node1.getLocation().dimension, leading, trailing));
    }
}
