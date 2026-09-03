package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.toroidalworld.VanillaInvokeTargets;
import com.toroidalworld.compat.create.CarriageEntityFrame;
import com.toroidalworld.compat.create.CreateTrackFold;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(value = Carriage.class, remap = false)
public abstract class CarriagePairMixin {
    @Shadow
    public abstract TravellingPoint getLeadingPoint();

    @WrapOperation(method = "getAnchorDiff",
            at = @At(value = "INVOKE",
                    target = VanillaInvokeTargets.VEC3_DISTANCE_TO))
    private double toroidal$foldAnchorSpan(Vec3 leading, Vec3 trailing, Operation<Double> original) {
        return original.call(leading,
                CreateTrackFold.nearestCopy(getLeadingPoint().node1.getLocation().dimension, leading, trailing));
    }

    @ModifyExpressionValue(method = "pivoted",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$pivotInPointFrame(Vec3 portalVec,
            @Local(argsOnly = true) Carriage.DimensionalCarriageEntity dce,
            @Local(argsOnly = true) ResourceKey<Level> dimension,
            @Local(ordinal = 0) Vec3 startVec) {
        return CreateTrackFold.nearestCopy(((CarriageEntityFrame) dce).toroidal$carriageLevel(), dimension,
                startVec, portalVec);
    }
}
