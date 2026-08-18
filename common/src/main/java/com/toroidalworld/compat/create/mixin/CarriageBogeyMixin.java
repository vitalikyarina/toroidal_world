package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.toroidalworld.compat.create.CreateTrackFold;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// Folding each edge is not folding what is built out of two of them. A bogey rides on two travelling points a wheel's
// spacing apart, and while it sits over a node those two points are on different edges — each answering in the frame of
// its own first node, which across the seam are a world apart. Everything the bogey then computes from the pair reads
// that world: its anchor is their midpoint, so a bogey astride the seam lands in the middle of the map; its angles come
// from their difference, so it points the wrong way; and its stress is their distance, which the train reads as a
// carriage torn apart.
//
// So the second point is brought to the copy nearest the first at each of the three, which is the one place the pair
// stops being two independent answers and becomes one quantity. Inside a single edge nothing folds and the arithmetic
// is Create's own, untouched.
@Mixin(value = CarriageBogey.class, remap = false)
public abstract class CarriageBogeyMixin {
    @Shadow
    public abstract ResourceKey<Level> getDimension();

    @WrapOperation(method = "getAnchorPosition(Z)Lnet/minecraft/world/phys/Vec3;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;add(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$foldAnchorMidpoint(Vec3 leading, Vec3 trailing, Operation<Vec3> original) {
        return original.call(leading, toroidal$nearest(leading, trailing));
    }

    // The trailing point's own answer, brought to the leading one already standing in a local above it.
    @ModifyExpressionValue(method = "updateAngles",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/trains/entity/TravellingPoint;getPosition(Lcom/simibubi/create/content/trains/graph/TrackGraph;)Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 1))
    private Vec3 toroidal$foldCoupledAngle(Vec3 trailing, @Local(ordinal = 0) Vec3 leading) {
        return toroidal$nearest(leading, trailing);
    }

    @WrapOperation(method = "getStress",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;distanceTo(Lnet/minecraft/world/phys/Vec3;)D"))
    private double toroidal$foldStressSpan(Vec3 leading, Vec3 trailing, Operation<Double> original) {
        return original.call(leading, toroidal$nearest(leading, trailing));
    }

    private Vec3 toroidal$nearest(Vec3 leading, Vec3 trailing) {
        return CreateTrackFold.nearestCopy(getDimension(), leading, trailing);
    }
}
