package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "com.simibubi.create.content.contraptions.minecart.CouplingPhysics", remap = false)
public class CouplingPhysicsMixin {
    @WrapOperation(method = "hardCollisionStep",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;distanceTo(Lnet/minecraft/world/phys/Vec3;)D"))
    private static double toroidal$stressBetweenTheNearCopies(Vec3 position, Vec3 otherPosition,
            Operation<Double> original, @Local(argsOnly = true) Level world) {
        return original.call(position, CreateSeamFold.nearestCopy(world, position, otherPosition));
    }

    @WrapOperation(method = "hardCollisionStep",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 0))
    private static Vec3 toroidal$linkBetweenTheNearCopies(Vec3 otherPosition, Vec3 position, Operation<Vec3> original,
            @Local(argsOnly = true) Level world) {
        return original.call(CreateSeamFold.nearestCopy(world, position, otherPosition), position);
    }

    @WrapOperation(method = "softCollisionStep",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;distanceTo(Lnet/minecraft/world/phys/Vec3;)D"))
    private static double toroidal$futureStressBetweenTheNearCopies(Vec3 position, Vec3 otherPosition,
            Operation<Double> original, @Local(argsOnly = true) Level world) {
        return original.call(position, CreateSeamFold.nearestCopy(world, position, otherPosition));
    }

    @WrapOperation(method = "softCollisionStep",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 0))
    private static Vec3 toroidal$futureLinkBetweenTheNearCopies(Vec3 otherPosition, Vec3 position,
            Operation<Vec3> original, @Local(argsOnly = true) Level world) {
        return original.call(CreateSeamFold.nearestCopy(world, position, otherPosition), position);
    }
}
