package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonTakeoffPhase;

@Mixin(DragonTakeoffPhase.class)
public class DragonTakeoffPhaseMixin {
    @WrapOperation(
            method = "doServerTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"))
    private boolean toroidal$takeoffClearanceThroughSeam(BlockPos eggPos, Position dragonPosition, double distance,
            Operation<Boolean> original) {
        return SeamRange.closerToCenterThan(((DragonPhaseAccessor) this).toroidal$dragon(), eggPos, dragonPosition,
                distance);
    }
}
