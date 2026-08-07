package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.animal.parrot.Parrot;

// A parrot dances while it is within 3.46 blocks of a playing jukebox and stops the moment it is not. The jukebox is
// remembered where it stands, so a parrot that crosses the seam beside one is measured a world from it and the dance
// ends on the next tick. Cosmetic, and the same reading in the same shape as everything else here.
@Mixin(Parrot.class)
public class ParrotMixin {
    @WrapOperation(
            method = "aiStep",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"))
    private boolean toroidal$jukeboxRangeThroughSeam(BlockPos jukeboxPos, Position bodyPosition, double distance,
            Operation<Boolean> original) {
        return SeamRange.closerToCenterThan((Parrot) (Object) this, jukeboxPos, bodyPosition, distance);
    }
}
