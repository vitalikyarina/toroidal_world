package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ambient.Bat;

// A bat picks a point a few blocks off and flies at it until it is within two blocks, then picks another. The point is
// walked out from the bat's own position, so no seam lies between them when it is chosen; the bat then crosses the
// boundary and is wrapped, and the point it was flying at is a world behind.
//
// Everything the tick asks about that point is wrong from there on, and it is not only the arrival. The heading is
// three raw differences reduced to their signs, so the bat turns around and flies away from a point it is beside — and
// the arrival that would have replaced the point cannot fire, so it keeps flying away until the one-in-thirty reroll
// happens to save it, a second and a half of a bat leaving the cave it was circling.
//
// Both fold on the one field they share. Every read in the tick becomes the copy nearest the bat — the arrival, the
// three deltas, and the emptiness check that discards a point turned to stone, which reads the same physical block
// either way. Only reads move; the two writes that choose a new point are left to store what they chose.
@Mixin(Bat.class)
public class BatMixin {
    @ModifyExpressionValue(
            method = "customServerAiStep",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/world/entity/ambient/Bat;targetPosition:Lnet/minecraft/core/BlockPos;",
                    opcode = Opcodes.GETFIELD))
    private @Nullable BlockPos toroidal$targetPositionThroughSeam(@Nullable BlockPos targetPosition) {
        return targetPosition == null ? null : SeamSteering.nearestCopy((Bat) (Object) this, targetPosition);
    }
}
