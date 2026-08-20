package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;

@Mixin(EnderDragon.class)
public class EnderDragonMixin {
    @ModifyExpressionValue(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/boss/enderdragon/phases/DragonPhaseInstance;"
                            + "getFlyTargetLocation()Lnet/minecraft/world/phys/Vec3;"))
    private @Nullable Vec3 toroidal$flyTargetThroughSeam(@Nullable Vec3 targetLocation) {
        EnderDragon self = (EnderDragon) (Object) this;
        return targetLocation == null ? null : SeamAim.nearestTo(self, targetLocation);
    }

    @WrapOperation(
            method = "getHeadLookVector",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;distToCenterSqr(Lnet/minecraft/core/Position;)D"))
    private double toroidal$eggDistanceThroughSeam(BlockPos eggPos, Position dragonPosition,
            Operation<Double> original) {
        return SeamRange.sqr((EnderDragon) (Object) this, Vec3.atCenterOf(eggPos), dragonPosition);
    }

    @ModifyExpressionValue(
            method = "knockBack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getX()D"))
    private double toroidal$shovedEntityX(double entityX) {
        return SeamAim.nearX((EnderDragon) (Object) this, entityX);
    }

    @ModifyExpressionValue(
            method = "knockBack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getZ()D"))
    private double toroidal$shovedEntityZ(double entityZ) {
        return SeamAim.nearZ((EnderDragon) (Object) this, entityZ);
    }
}
