package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "com.simibubi.create.content.contraptions.minecart.capability.CapabilityMinecartController",
        remap = false)
public class CapabilityMinecartControllerMixin {
    @WrapOperation(method = "handleKilledMinecart",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;add(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 toroidal$dropBetweenTheNearCopies(Vec3 position, Vec3 removedPosition,
            Operation<Vec3> original, @Local(argsOnly = true) Level world) {
        return original.call(position, CreateSeamFold.nearestCopy(world, position, removedPosition));
    }
}
