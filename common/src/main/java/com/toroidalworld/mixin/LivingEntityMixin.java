package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.entity.SeamAim;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @ModifyVariable(method = "startSleeping", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$wrapBedPosition(BlockPos bedPosition) {
        WorldFold transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        return transformer == null ? bedPosition : transformer.fold(bedPosition);
    }

    @ModifyVariable(
            method = "knockback(DDD)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double toroidal$knockbackDirX(double xd) {
        return SeamAim.foldX((LivingEntity) (Object) this, xd);
    }

    @ModifyVariable(
            method = "knockback(DDD)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private double toroidal$knockbackDirZ(double zd) {
        return SeamAim.foldZ((LivingEntity) (Object) this, zd);
    }

    @ModifyExpressionValue(
            method = "applyItemBlocking",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$blockConeThroughSeam(Vec3 attackDirection) {
        return SeamAim.foldDelta((LivingEntity) (Object) this, attackDirection);
    }

    @ModifyVariable(
            method = "hasLineOfSight(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/ClipContext$Block;Lnet/minecraft/world/level/ClipContext$Fluid;D)Z",
            at = @At("STORE"), ordinal = 1)
    private Vec3 toroidal$sightTargetThroughSeam(Vec3 to) {
        LivingEntity self = (LivingEntity) (Object) this;
        WorldFold transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        return transformer == null ? to : transformer.nearestCopy(self.position(), to);
    }

    @ModifyExpressionValue(
            method = "checkFallDamage(DZLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getX()D"))
    private double toroidal$landingXNearBlock(double x, @Local(argsOnly = true) BlockPos pos) {
        return toroidal$nearestLandingCoordinate(Direction.Axis.X, x, pos);
    }

    @ModifyExpressionValue(
            method = "checkFallDamage(DZLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D"))
    private double toroidal$landingZNearBlock(double z, @Local(argsOnly = true) BlockPos pos) {
        return toroidal$nearestLandingCoordinate(Direction.Axis.Z, z, pos);
    }

    @ModifyExpressionValue(
            method = "checkFallDamage(DZLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;blockPosition()Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$landingBlockNearBlock(BlockPos entityPos, @Local(argsOnly = true) BlockPos pos) {
        WorldFold transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        return transformer == null ? entityPos : transformer.nearestCopy(pos, entityPos);
    }

    @Unique
    private double toroidal$nearestLandingCoordinate(Direction.Axis axis, double coordinate, BlockPos landingBlock) {
        WorldFold transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        if (transformer == null) {
            return coordinate;
        }

        return transformer.blockDomain(axis).unwrapAround(landingBlock.get(axis) + 0.5, coordinate);
    }
}
