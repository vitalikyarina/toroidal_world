package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BellBlockEntity;

// A rung bell gathers everything living within 48 blocks and then measures each of them again, at 32 to be heard and at
// 48 to be lit. The gathering already reaches across the seam — the level splits an entity box that crosses the bounds
// and searches both pieces — so the far half of a village does arrive in the list, and is then dropped by four raw
// differences against the bell's own position.
//
// What is lost is the whole point of ringing one. Villagers on the far side never receive the heard-bell memory, so they
// do not break off and hide; the resonance never sees the raiders standing next to the bell, so none of them are
// outlined; and the particle count is scaled by a tally that comes back zero.
@Mixin(BellBlockEntity.class)
public class BellBlockEntityMixin {
    @WrapOperation(
            method = "updateEntities",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"))
    private boolean toroidal$hearingRangeThroughSeam(BlockPos bellPos, Position bodyPosition, double distance,
            Operation<Boolean> original, @Local LivingEntity body) {
        return SeamRange.closerToCenterThan(body, bellPos, bodyPosition, distance);
    }

    // The raider readings are one question asked in three places — may the bell resonate, who is outlined by it, how
    // dense the particles are — and each holds the entity it measures, so one handler answers all three.
    @WrapOperation(
            method = { "areRaidersNearby", "isRaiderWithinRange", "lambda$showBellParticles$0" },
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"),
            expect = 3)
    private static boolean toroidal$raiderRangeThroughSeam(BlockPos bellPos, Position bodyPosition, double distance,
            Operation<Boolean> original, @Local LivingEntity body) {
        return SeamRange.closerToCenterThan(body, bellPos, bodyPosition, distance);
    }
}
