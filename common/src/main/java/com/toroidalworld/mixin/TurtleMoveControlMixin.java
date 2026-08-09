package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Turtle;

// Swimming speed is halved once the turtle is more than sixteen blocks from home, which is what makes it hurry over the
// last stretch and dawdle out at sea. Read raw across the seam it is always far, so a turtle beside its own beach
// crawls at half speed for the whole approach.
//
// The move control reaches its turtle only through the field it inherits from the base control, and the reading itself
// is the one call that names the turtle out loud: wrapping the home getter takes the receiver from the invoke, which is
// the turtle, so no accessor is needed to reach it. The home comes back as its copy nearest the turtle and vanilla's
// own comparison runs on that.
@Mixin(targets = "net.minecraft.world.entity.animal.Turtle$TurtleMoveControl")
public class TurtleMoveControlMixin {
    @WrapOperation(
            method = "updateSpeed",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/animal/Turtle;getHomePos()Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$homeThroughSeam(Turtle turtle, Operation<BlockPos> original) {
        return SeamSteering.nearestCopy(turtle, original.call(turtle));
    }
}
