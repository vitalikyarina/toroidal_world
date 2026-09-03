package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.toroidalworld.VanillaInvokeTargets;
import com.toroidalworld.compat.create.CreateTrackFold;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

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

    @ModifyExpressionValue(method = "updateAngles",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/trains/entity/TravellingPoint;getPosition(Lcom/simibubi/create/content/trains/graph/TrackGraph;)Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 1))
    private Vec3 toroidal$foldCoupledAngle(Vec3 trailing, @Local(ordinal = 0) Vec3 leading) {
        return toroidal$nearest(leading, trailing);
    }

    @WrapOperation(method = "getStress",
            at = @At(value = "INVOKE",
                    target = VanillaInvokeTargets.VEC3_DISTANCE_TO))
    private double toroidal$foldStressSpan(Vec3 leading, Vec3 trailing, Operation<Double> original) {
        return original.call(leading, toroidal$nearest(leading, trailing));
    }

    private Vec3 toroidal$nearest(Vec3 leading, Vec3 trailing) {
        return CreateTrackFold.nearestCopy(getDimension(), leading, trailing);
    }
}
