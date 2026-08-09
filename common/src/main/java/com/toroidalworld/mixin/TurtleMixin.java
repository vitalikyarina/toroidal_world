package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.animal.Turtle;

// A turtle sinks while it swims, unless it is heading home and already close to the beach — that is what keeps it near
// the surface for the last twenty blocks instead of scraping along the sea floor. The home is written down where it
// lies and the turtle is wrapped, so a beach across the seam never reads as close and the turtle arrives underneath its
// own nesting ground.
@Mixin(Turtle.class)
public class TurtleMixin {
    @WrapOperation(
            method = "travelInWater",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"))
    private boolean toroidal$homeApproachThroughSeam(BlockPos homePos, Position bodyPosition, double distance,
            Operation<Boolean> original) {
        return SeamRange.closerToCenterThan((Turtle) (Object) this, homePos, bodyPosition, distance);
    }
}
