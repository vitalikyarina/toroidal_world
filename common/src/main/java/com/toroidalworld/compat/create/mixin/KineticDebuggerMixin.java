package com.toroidalworld.compat.create.mixin;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.KineticDebugger;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.toroidalworld.compat.create.client.CreateClientFrame;

import net.minecraft.core.BlockPos;

@Mixin(value = KineticDebugger.class, remap = false)
public abstract class KineticDebuggerMixin {
    @ModifyExpressionValue(method = "tick",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;"
                            + "source:Lnet/minecraft/core/BlockPos;"))
    private static @Nullable BlockPos toroidal$sourceInTheBlockEntityFrame(@Nullable BlockPos canonical,
            @Local KineticBlockEntity blockEntity) {
        if (canonical == null) {
            return null;
        }

        return CreateClientFrame.nearestCopy(blockEntity.getLevel(), canonical);
    }
}
