package com.toroidalworld.compat.create.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.fluids.transfer.FluidSplashPacket;
import com.toroidalworld.compat.create.client.CreateClientFrame;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

@Mixin(value = FluidSplashPacket.class, remap = false)
public abstract class FluidSplashPacketMixin {
    @ModifyExpressionValue(method = "handle",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lcom/simibubi/create/content/fluids/transfer/FluidSplashPacket;pos:Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$splashInTheViewerFrame(BlockPos canonical) {
        return CreateClientFrame.nearestCopy(Minecraft.getInstance().level, canonical);
    }
}
