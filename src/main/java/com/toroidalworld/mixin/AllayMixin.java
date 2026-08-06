package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.animal.allay.Allay;

// The allay's dance holds while the jukebox that started it is inside the notification radius of the play event. The
// position is the one the event carried, in the world, and the allay is wrapped — so across the seam it stops dancing
// on the next tick, and an allay that is not dancing cannot be duplicated with an amethyst shard.
@Mixin(Allay.class)
public class AllayMixin {
    @WrapOperation(
            method = "shouldStopDancing",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"))
    private boolean toroidal$jukeboxRangeThroughSeam(BlockPos jukeboxPos, Position bodyPosition, double distance,
            Operation<Boolean> original) {
        return SeamRange.closerToCenterThan((Allay) (Object) this, jukeboxPos, bodyPosition, distance);
    }
}
