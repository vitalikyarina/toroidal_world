package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.redstone.link.controller.LecternControllerBlockEntity;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(value = LecternControllerBlockEntity.class, remap = false)
public class LecternControllerBlockEntityMixin {
    @WrapOperation(method = "playerInRange",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"))
    private static double toroidal$foldLecternReach(Vec3 eye, Vec3 centre, Operation<Double> original,
            Player player, Level world) {
        return original.call(eye, CreateSeamFold.nearestCopy(world, eye, centre));
    }
}
