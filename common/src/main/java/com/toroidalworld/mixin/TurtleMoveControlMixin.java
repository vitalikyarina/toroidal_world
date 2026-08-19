package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Turtle;

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
