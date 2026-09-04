package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.entity.SeamRange;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.Vec3;

@Mixin(VibrationSystem.Listener.class)
public class VibrationSystemListenerMixin {
    @WrapOperation(
            method = "scheduleVibration",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO))
    private double toroidal$travelDistThroughSeam(Vec3 origin, Vec3 dest, Operation<Double> original,
            @Local(argsOnly = true) ServerLevel level) {
        return Math.sqrt(SeamRange.sqr(level, origin, dest));
    }

    @ModifyVariable(method = "isOccluded", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private static Vec3 toroidal$unwrapOcclusionTarget(Vec3 dest, @Local(argsOnly = true, ordinal = 0) Vec3 origin,
            @Local(argsOnly = true) Level level) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            return dest;
        }

        return transformer.nearestCopy(origin, dest);
    }
}
