package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.toroidalworld.compat.create.CreateTrackFold;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "com.simibubi.create.content.contraptions.minecart.CouplingHandler", remap = false)
public class CouplingHandlerMixin {
    @WrapOperation(method = "tryToCoupleCarts",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;distanceTo(Lnet/minecraft/world/phys/Vec3;)D"))
    private static double toroidal$couplingSpanBetweenTheNearCopies(Vec3 position, Vec3 otherPosition,
            Operation<Double> original, @Local(argsOnly = true) Level world) {
        return original.call(position, CreateTrackFold.nearestCopy(world, position, otherPosition));
    }
}
