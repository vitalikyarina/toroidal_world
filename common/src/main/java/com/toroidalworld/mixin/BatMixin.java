package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ambient.Bat;

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
