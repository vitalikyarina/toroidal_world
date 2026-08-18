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

// The carriage's own reading of how far its two ends have drifted apart, which the train uses to decide it is being
// stretched. The two anchors come from two bogeys, each already folded within itself, and across the seam they are still
// named from two frames — so the untouched distance is a world and the train reads a carriage torn in half.
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
