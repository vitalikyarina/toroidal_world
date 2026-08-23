package com.toroidalworld.compat.create.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.contraptions.glue.GlueEffectPacket;
import com.toroidalworld.compat.create.client.CreateClientFrame;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

@Mixin(value = GlueEffectPacket.class, remap = false)
public abstract class GlueEffectPacketMixin {
    @ModifyExpressionValue(method = "handle",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lcom/simibubi/create/content/contraptions/glue/GlueEffectPacket;pos:Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$gluedBlockInTheViewerFrame(BlockPos canonical) {
        return CreateClientFrame.nearestCopy(Minecraft.getInstance().level, canonical);
    }
}
