package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonTakeoffPhase;

// Leaving the podium is timed by distance: the dragon climbs away until it is ten blocks clear of the egg, and only
// then hands over to the circling phase. The egg is derived from the fight's origin, in the world, while the dragon is
// wrapped — so across the seam the ten blocks are already clear the first time the phase looks, which is the tick after
// the one it spends picking a path, and the takeoff ends before the dragon has left the ground.
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
