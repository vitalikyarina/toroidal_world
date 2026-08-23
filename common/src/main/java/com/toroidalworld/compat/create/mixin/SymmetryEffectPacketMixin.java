package com.toroidalworld.compat.create.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.equipment.symmetryWand.SymmetryEffectPacket;
import com.toroidalworld.compat.create.client.CreateClientFrame;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

@Mixin(value = SymmetryEffectPacket.class, remap = false)
public abstract class SymmetryEffectPacketMixin {
    @ModifyExpressionValue(method = "handle",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lcom/simibubi/create/content/equipment/symmetryWand/SymmetryEffectPacket;"
                            + "mirror:Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$mirrorInTheViewerFrame(BlockPos canonical) {
        return CreateClientFrame.nearestCopy(Minecraft.getInstance().level, canonical);
    }

    @ModifyArg(method = "handle",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/equipment/symmetryWand/SymmetryHandler;"
                            + "drawEffect(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)V"),
            index = 1)
    private BlockPos toroidal$placedBlockInTheViewerFrame(BlockPos canonical) {
        return CreateClientFrame.nearestCopy(Minecraft.getInstance().level, canonical);
    }
}
