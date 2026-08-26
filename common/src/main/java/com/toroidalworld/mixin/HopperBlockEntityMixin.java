package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
    @WrapOperation(
            method = "entityInside(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/entity/HopperBlockEntity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;move(DDD)Lnet/minecraft/world/phys/AABB;"))
    private static AABB toroidal$boxAgainstNearestCopy(AABB box, double dx, double dy, double dz, Operation<AABB> original,
            @Local(argsOnly = true) BlockPos pos, @Local(argsOnly = true) Entity entity) {
        BlockPos nearest = SeamAim.nearestTo(entity, pos);
        return nearest == pos
                ? original.call(box, dx, dy, dz)
                : original.call(box, (double) -nearest.getX(), dy, (double) -nearest.getZ());
    }
}
